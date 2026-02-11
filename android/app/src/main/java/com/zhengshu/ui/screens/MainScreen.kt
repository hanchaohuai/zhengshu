package com.zhengshu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhengshu.data.model.RiskLevel
import com.zhengshu.ui.viewmodel.MainViewModel
import com.zhengshu.ui.viewmodel.MainTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val riskAlertState by viewModel.riskAlertState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("证枢") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = getTabIcon(tab),
                                contentDescription = tab.displayName
                            )
                        },
                        label = { Text(tab.displayName) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.selectedTab) {
                MainTab.Home -> HomeScreen(viewModel)
                MainTab.Evidence -> EvidenceScreen()
                MainTab.Legal -> LegalScreen()
                MainTab.Judiciary -> JudiciaryScreen()
                MainTab.Hardware -> HardwareScreen()
                MainTab.Settings -> SettingsScreen()
            }

            riskAlertState?.let { alert ->
                RiskAlertDialog(
                    alert = alert,
                    onDismiss = { viewModel.dismissRiskAlert() },
                    onStartEvidence = { viewModel.startEvidenceCollection() },
                    onMarkFalsePositive = { viewModel.markAsFalsePositive() }
                )
            }
        }
    }
}

@Composable
fun getTabIcon(tab: MainTab) = when (tab) {
    MainTab.Home -> Icons.Default.Home
    MainTab.Evidence -> Icons.Default.Folder
    MainTab.Legal -> Icons.Default.Description
    MainTab.Judiciary -> Icons.Default.Gavel
    MainTab.Hardware -> Icons.Default.Usb
    MainTab.Settings -> Icons.Default.Settings
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "风险识别服务",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.isCollectingEvidence) {
                        "正在运行中..."
                    } else {
                        "已停止"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "最近风险检测",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                RiskList()
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "快速操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startEvidenceCollection() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("启动存证")
                    }
                    Button(
                        onClick = { viewModel.stopEvidenceCollection() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("停止存证")
                    }
                }
            }
        }
    }
}

@Composable
fun RiskList() {
    val mockRisks = listOf(
        RiskItem("投资返利", RiskLevel.HIGH, "2024-01-10 14:30"),
        RiskItem("兼职刷单", RiskLevel.MEDIUM, "2024-01-09 10:15"),
        RiskItem("中奖通知", RiskLevel.HIGH, "2024-01-08 16:45")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mockRisks) { risk ->
            RiskItemCard(risk)
        }
    }
}

@Composable
fun RiskItemCard(risk: RiskItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (risk.riskLevel) {
                RiskLevel.HIGH -> Color(0xFFFFD32F2F)
                RiskLevel.MEDIUM -> Color(0xFFF57C00)
                RiskLevel.LOW -> Color(0xFFF9A825)
                RiskLevel.NONE -> Color(0xFF4CAF50)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = risk.keyword,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = risk.time,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

data class RiskItem(
    val keyword: String,
    val riskLevel: RiskLevel,
    val time: String
)

@Composable
fun EvidenceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "存证管理",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无证据包",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LegalScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "法律文书",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无文书",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun JudiciaryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "司法平台",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无对接记录",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun HardwareScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "硬件同步",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未连接硬件设备",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "设置选项",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}