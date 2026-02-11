package com.zhengshu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Article
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
    MainTab.Evidence -> Icons.Default.FolderOpen
    MainTab.Legal -> Icons.Default.Article
    MainTab.Judiciary -> Icons.Default.Gavel
    MainTab.Hardware -> Icons.Default.Usb
    MainTab.Settings -> Icons.Default.Settings
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
