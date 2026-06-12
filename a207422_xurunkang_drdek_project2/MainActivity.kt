package com.example.a207422_xurunkang_drdek_project1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var viewModel: CoffeeViewModel? = null

    // 记录上一次摇晃的时间，防止高频触发（防抖）
    private var lastUpdate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化传感器
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                val vm: CoffeeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CoffeeViewModel(application) as T
                        }
                    }
                )
                viewModel = vm

                MainAppScreen(navController, vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // 摇一摇硬件判定
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val curTime = System.currentTimeMillis()

            // 防抖：限制每 800 毫秒内只能触发一次
            if ((curTime - lastUpdate) > 800) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                
                val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

                
                if (acceleration > 3f) {
                    lastUpdate = curTime
                    // 弹出 Toast 提示
                    Toast.makeText(this, "🎲 Hardware Shake Detected!", Toast.LENGTH_SHORT).show()
                    viewModel?.recommendRandomBean()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 留空即可，符合接口规范
    }
}



@Composable
fun MainAppScreen(navController: NavHostController, viewModel: CoffeeViewModel) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { BottomNavBar(navController, currentRoute) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(viewModel, onNext = { navController.navigate("prepare") }) }
            composable("prepare") { PrepareScreen(viewModel, onNext = { navController.navigate("add") }) }
            composable("add") {
                AddBeanScreen(
                    viewModel = viewModel,
                    onNext = { navController.navigate("list") }
                )
            }
            composable("list") { ListScreen(viewModel) }
            // 🌟 第 7 屏网络条形码查询界面的注册
            composable("api_fetch") { ApiFetchScreen(viewModel) }
            composable("impact") {
                ImpactScreen(
                    viewModel = viewModel,
                    onNavigateToCommunity = { navController.navigate("community") }
                )
            }
            composable("community") { CommunityScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarItem("🌍", "Home", currentRoute == "home") { navController.navigate("home") }
        NavBarItem("☕", "Brew", currentRoute == "prepare") { navController.navigate("prepare") }
        NavBarItem("➕", "Add", currentRoute == "add") { navController.navigate("add") }
        NavBarItem("📋", "List", currentRoute == "list") { navController.navigate("list") }
        NavBarItem("🔍", "API", currentRoute == "api_fetch") { navController.navigate("api_fetch") }
        NavBarItem("🌱", "Impact", currentRoute == "impact") { navController.navigate("impact") }
        NavBarItem("☁️", "Cloud", currentRoute == "community") { navController.navigate("community") }
    }
}

@Composable
fun NavBarItem(icon: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(label, fontSize = 10.sp, color = color, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}