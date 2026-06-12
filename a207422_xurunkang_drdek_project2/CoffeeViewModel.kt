package com.example.a207422_xurunkang_drdek_project1

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers // 🌟 新增导入：后台线程调度器
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 统一复用你的数据模型
data class BeanItem(val name: String, val origin: String)

data class CloudCommunityData(
    val userId: String = "",
    val lastMethod: String = "",
    val cups: Int = 0
)

data class CoffeeAppState(
    val selectedMethod: String = "None Selected",
    val plasticCupsSaved: Int = 0,
    val apiBeanName: String = "",
    val apiOrigin: String = "",
    val isApiLoading: Boolean = false,
    val recommendedBean: BeanItem? = null
)

class CoffeeViewModel(application: Application) : AndroidViewModel(application) {

    // 🌟 1. 初始化真正的物理本地 Room 数据库
    private val database = AppDatabase.getDatabase(application)
    private val coffeeDao = database.coffeeDao()

    // 2. 本地持久化配置（仅用于保存个人的环保杯数）
    private val prefs = application.getSharedPreferences("coffee_local_db", Context.MODE_PRIVATE)

    // 🌟 3. 真正的 Room 数据流：直接从本地 Room 数据库实时监听咖啡豆列表
    val roomBeansList: Flow<List<CoffeeEntity>> = coffeeDao.getAllBeans()

    private val _uiState = MutableStateFlow(CoffeeAppState(plasticCupsSaved = prefs.getInt("cups_saved", 0)))
    val uiState: StateFlow<CoffeeAppState> = _uiState.asStateFlow()

    // 🌟 4. 云端 Firestore 数据流
    private val _firebaseCloudList = MutableStateFlow<List<CloudCommunityData>>(emptyList())
    val firebaseCloudList: StateFlow<List<CloudCommunityData>> = _firebaseCloudList.asStateFlow()

    // 自动生成的随机设备/用户ID，确保不同设备在云端独立
    private val deviceUserId = prefs.getString("device_user_id", null) ?: "User_${(1000..9999).random()}".also {
        prefs.edit().putString("device_user_id", it).apply()
    }

    init {
        // 🌟 5. App 启动时，自动开启 Firebase Firestore 远程数据实时监听
        listenToCloudCommunityFirestore()
    }

    fun selectMethod(method: String) {
        _uiState.update {
            val newCups = it.plasticCupsSaved + 1
            prefs.edit().putInt("cups_saved", newCups).apply()
            it.copy(selectedMethod = method, plasticCupsSaved = newCups)
        }
    }

    // 🌟 6. 真正的 Room 数据库插入逻辑
    fun addBeanLocalRoom(name: String, origin: String) {
        // 🛠️ 核心修复：加入 Dispatchers.IO，让数据库写入操作在后台线程执行，彻底解决闪退！
        viewModelScope.launch(Dispatchers.IO) {
            coffeeDao.insertBean(CoffeeEntity(name = name, origin = origin))
        }
    }

    // 🌟 7. 核心完善：真正的 Firebase Firestore 双向同步（上传 + 实时下拉监听）
    fun syncImpactToFirebaseFirestore() {
        val currentMethod = _uiState.value.selectedMethod
        val currentCups = _uiState.value.plasticCupsSaved

        if (currentCups > 0) {
            try {
                val db = FirebaseFirestore.getInstance()

                // 将环保数据打包，准备上传
                val cloudData = hashMapOf(
                    "userId" to deviceUserId,
                    "lastMethod" to currentMethod,
                    "cupsSaved" to currentCups,
                    "timestamp" to System.currentTimeMillis()
                )

                // 真正将数据推送到 Google 服务器的 "GlobalEcoImpact" 集合中
                db.collection("GlobalEcoImpact")
                    .document(deviceUserId)
                    .set(cloudData)
                    .addOnSuccessListener {
                        println("Successfully uploaded to Firebase Firestore!")
                    }
                    .addOnFailureListener { e ->
                        println("Error uploading to Firebase: ${e.message}")
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🌟 8. 核心添加：利用 SnapshotListener 捕获云端全网用户的最新环保数据
    private fun listenToCloudCommunityFirestore() {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("GlobalEcoImpact")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Firestore snapshot listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        // 解析云端所有设备的文档，映射为社区列表对象
                        val list = snapshot.documents.mapNotNull { doc ->
                            val uId = doc.getString("userId") ?: return@mapNotNull null
                            val method = doc.getString("lastMethod") ?: "Unknown"
                            val cupsSaved = doc.getLong("cupsSaved")?.toInt() ?: 0
                            CloudCommunityData(userId = uId, lastMethod = method, cups = cupsSaved)
                        }
                        // 更新全局 StateFlow，UI 界面会自动刷新
                        _firebaseCloudList.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // ⬇️ 摇一摇传感器推荐逻辑 & 摇号池 ⬇️
    // ==========================================
    fun recommendRandomBean() {
        val demoBeans = listOf(
            BeanItem("Classic Espresso", "Brazil"),
            BeanItem("Fruity Yirgacheffe", "Ethiopia"),
            BeanItem("Geisha Washed", "Panama"),
            BeanItem("Dark Mandheling", "Indonesia"),
            BeanItem("Decaf Supremo", "Colombia")
        )
        val currentRec = _uiState.value.recommendedBean
        val availableChoices = demoBeans.filter { it != currentRec }
        if (availableChoices.isNotEmpty()) {
            _uiState.update { it.copy(recommendedBean = availableChoices.random()) }
        }
    }

    // ==========================================
    // ⬇️ Retrofit 网络 API 逻辑 ⬇️
    // ==========================================
    fun fetchDataFromInternetApi(barcode: String, onScanComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApiLoading = true) }
            try {
                val response = NetworkModule.api.getProductByBarcode(barcode)
                val fetchedName = response.product?.product_name ?: "Unknown Coffee"
                val fetchedBrand = response.product?.brands ?: "Unknown Brand"
                _uiState.update {
                    it.copy(
                        apiBeanName = "$fetchedBrand $fetchedName",
                        apiOrigin = "Success from Retrofit API",
                        isApiLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(apiBeanName = "API Connection Failed", apiOrigin = "Error", isApiLoading = false) }
            }
            onScanComplete()
        }
    }

    fun clearApiCache() { _uiState.update { it.copy(apiBeanName = "", apiOrigin = "") } }
}