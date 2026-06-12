package com.example.a207422_xurunkang_drdek_project1

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 页面 1: Home Screen
// ==========================================
@Composable
fun HomeScreen(viewModel: CoffeeViewModel, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌍", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Eco Brew Tracker v2", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("SDG 12: Zero Plastic Waste", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Start Brewing at Home")
        }
    }
}

// ==========================================
// 页面 2: Prepare Screen
// ==========================================
@Composable
fun PrepareScreen(viewModel: CoffeeViewModel, onNext: () -> Unit) {
    val appState by viewModel.uiState.collectAsState()
    val methods = listOf("V60 Pour Over", "French Press", "AeroPress")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Choose Method", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Brewing at home saves plastic cups!", color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(24.dp))

        methods.forEach { method ->
            val isSelected = appState.selectedMethod == method
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        viewModel.selectMethod(method)
                        onNext()
                    }
            ) {
                Text(method, modifier = Modifier.padding(20.dp), fontSize = 18.sp)
            }
        }
    }
}

// ==========================================
// 页面 3: Add Bean Screen
// ==========================================
@Composable
fun AddBeanScreen(viewModel: CoffeeViewModel, onNext: () -> Unit) {
    val appState by viewModel.uiState.collectAsState()
    var beanName by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }

    // 监听传感器逻辑：只要摇一摇晃动触发，自动填入推荐
    LaunchedEffect(appState.recommendedBean) {
        appState.recommendedBean?.let { bean ->
            beanName = bean.name
            origin = bean.origin
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add New Beans", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Selected Method: ${appState.selectedMethod}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 🎲 模拟硬件摇一摇按钮 (同时完美支持物理传感器晃动手机)
        Button(
            onClick = { viewModel.recommendRandomBean() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("🎲 Shake Phone or Click to Random Recommend")
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = beanName, onValueChange = { beanName = it }, label = { Text("Bean Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = origin, onValueChange = { origin = it }, label = { Text("Origin") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (beanName.isNotBlank() && origin.isNotBlank()) {
                    // 🌟 插入到后台线程保护的本地 Room 数据库
                    viewModel.addBeanLocalRoom(beanName, origin)
                    beanName = ""
                    origin = ""
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save to Private Room DB")
        }
    }
}

// ==========================================
// 页面 4: List Screen (全面接入 Room Flow 监听)
// ==========================================
@Composable
fun ListScreen(viewModel: CoffeeViewModel) {
    // 🌟 实时监听真实的 Room 实体数据流
    val roomBeans by viewModel.roomBeansList.collectAsState(initial = emptyList())
    val appState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("My Coffee Collection (Room)", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Current Brew: ${appState.selectedMethod}", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        if (roomBeans.isEmpty()) {
            Text("No data inside offline Room Database. Add some!", color = Color.Gray)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(roomBeans) { bean ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(bean.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Origin: ${bean.origin}", color = MaterialTheme.colorScheme.primary)
                            Text("Database Key ID: #${bean.id}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 页面 5: ApiFetchScreen (网络 API 核心展示)
// ==========================================
@Composable
fun ApiFetchScreen(viewModel: CoffeeViewModel) {
    val appState by viewModel.uiState.collectAsState()
    // 默认提供全球通用可查询条形码
    var barcodeInput by remember { mutableStateOf("8000070020141") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Internet Bean Fetcher", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Retrofit Connects to OpenFoodFacts REST API", color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = barcodeInput,
            onValueChange = { barcodeInput = it },
            label = { Text("Enter Coffee Product Barcode") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { if (!appState.isApiLoading) viewModel.fetchDataFromInternetApi(barcodeInput) {} },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (appState.isApiLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Fetch Data via Retrofit API")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (appState.apiBeanName.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Result Obtained:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Product Info: ${appState.apiBeanName}", fontSize = 16.sp)
                    Text("Status: ${appState.apiOrigin}", fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.addBeanLocalRoom(appState.apiBeanName, "Fetched via API")
                            viewModel.clearApiCache()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Save API Data into Room DB")
                    }
                }
            }
        }
    }
}

// ==========================================
// 页面 6: Impact Screen (个人数据同步至云端)
// ==========================================
@Composable
fun ImpactScreen(viewModel: CoffeeViewModel, onNavigateToCommunity: () -> Unit) {
    val appState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌱", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your Personal Eco Impact", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Plastic Cups Saved:", fontSize = 16.sp)
                Text("${appState.plasticCupsSaved}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.syncImpactToFirebaseFirestore()
                onNavigateToCommunity()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Sync & Share to Cloud Hub ☁️")
        }
    }
}

// ==========================================
// 页面 7: Community Screen (云端多端真实互动中心)
// ==========================================
@Composable
fun CommunityScreen(viewModel: CoffeeViewModel) {
    // 🌟 核心：实时观察从 Firebase 倒灌进来的全网社区多端共享数据流
    val cloudDataList by viewModel.firebaseCloudList.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Cloud Community Hub", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Real-time Firebase Firestore Syncing", color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(24.dp))

        if (cloudDataList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fetching cloud eco impacts...", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(cloudDataList) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Device Node: ${entry.userId}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Pref Method: ${entry.lastMethod}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Saved: ${entry.cups} 🌍", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}