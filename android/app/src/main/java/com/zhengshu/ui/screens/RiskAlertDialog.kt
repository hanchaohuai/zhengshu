package com.zhengshu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zhengshu.data.model.RiskLevel
import com.zhengshu.ui.viewmodel.RiskAlertState

@Composable
fun RiskAlertDialog(
    alert: RiskAlertState,
    onDismiss: () -> Unit,
    onStartEvidence: () -> Unit,
    onMarkFalsePositive: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "警告",
                    tint = when (alert.riskLevel) {
                        RiskLevel.HIGH -> Color(0xFFFFD32F2F)
                        RiskLevel.MEDIUM -> Color(0xFFF57C00)
                        RiskLevel.LOW -> Color(0xFFF9A825)
                        RiskLevel.NONE -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "风险预警",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getRiskLevelText(alert.riskLevel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (alert.riskLevel) {
                        RiskLevel.HIGH -> Color(0xFFFFD32F2F)
                        RiskLevel.MEDIUM -> Color(0xFFF57C00)
                        RiskLevel.LOW -> Color(0xFFF9A825)
                        RiskLevel.NONE -> Color(0xFF4CAF50)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "风险原因",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = alert.riskReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (alert.detectedKeywords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "检测到的关键词",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = alert.detectedKeywords.joinToString("、"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("忽略")
                    }

                    Button(
                        onClick = onMarkFalsePositive,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("标记误报")
                    }

                    Button(
                        onClick = onStartEvidence,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (alert.riskLevel) {
                                RiskLevel.HIGH -> Color(0xFFFFD32F2F)
                                RiskLevel.MEDIUM -> Color(0xFFF57C00)
                                RiskLevel.LOW -> Color(0xFFF9A825)
                                RiskLevel.NONE -> Color(0xFF4CAF50)
                            }
                        )
                    ) {
                        Text("启动存证")
                    }
                }
            }
        }
    }
}

@Composable
fun getRiskLevelText(riskLevel: RiskLevel): String {
    return when (riskLevel) {
        RiskLevel.HIGH -> "高风险"
        RiskLevel.MEDIUM -> "中风险"
        RiskLevel.LOW -> "低风险"
        RiskLevel.NONE -> "无风险"
    }
}
