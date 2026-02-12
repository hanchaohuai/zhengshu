package com.zhengshu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
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
import com.zhengshu.ui.viewmodel.RiskAlertState

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
fun RiskAlertDialog(
    alert: RiskAlertState,
    onDismiss: () -> Unit,
    onStartEvidence: () -> Unit,
    onMarkFalsePositive: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "风险预警",
                color = when (alert.riskLevel) {
                    com.zhengshu.data.model.RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                    com.zhengshu.data.model.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    com.zhengshu.data.model.RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
                    com.zhengshu.data.model.RiskLevel.NONE -> MaterialTheme.colorScheme.primary
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = alert.riskReason,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (alert.detectedKeywords.isNotEmpty()) {
                    Text(
                        text = "检测关键词: ${alert.detectedKeywords.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "置信度: ${(alert.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onStartEvidence) {
                Text("开始存证")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onMarkFalsePositive) {
                    Text("误报")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
}

@Composable
fun getTabIcon(tab: MainTab) = when (tab) {
    MainTab.Home -> Icons.Default.Home
    MainTab.Evidence -> Icons.Default.Folder
    MainTab.Legal -> Icons.Default.Description
    MainTab.Judiciary -> Icons.Default.Info
    MainTab.Hardware -> Icons.Default.Info
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
                containerColor = when (uiState.currentRiskLevel) {
                    com.zhengshu.data.model.RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                    com.zhengshu.data.model.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    com.zhengshu.data.model.RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
                    com.zhengshu.data.model.RiskLevel.NONE -> MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "当前风险等级",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.currentRiskLevel.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                    text = "风险识别统计",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "今日检测次数: ${uiState.detectionCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "高风险预警: ${uiState.highRiskCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Button(
            onClick = { viewModel.startDetection() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isDetecting
        ) {
            Text(if (uiState.isDetecting) "检测中..." else "开始检测")
        }
    }
}

@Composable
fun EvidenceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "证据管理",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "暂无证据",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun LegalScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "法律咨询",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "法律援助热线",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "12348",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun JudiciaryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "司法存证",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "区块链存证",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将证据上链存储，确保不可篡改",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun HardwareScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "硬件管理",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "USB设备监控",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "监控USB设备连接，防止数据泄露",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "风险检测设置",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "检测灵敏度: 高",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "自动存证: 开启",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}