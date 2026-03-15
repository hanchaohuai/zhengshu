package com.zhengshu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.data.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRiskScreen(
    messages: List<ChatMessage>,
    riskLevels: Map<String, RiskLevel>,
    onBackClick: () -> Unit,
    onMessageClick: (ChatMessage) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<RiskLevel?>(null) }
    
    val filteredMessages = if (selectedFilter != null) {
        messages.filter { riskLevels[it.id] == selectedFilter }
    } else {
        messages
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天风险监控") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
            
            if (filteredMessages.isEmpty()) {
                EmptyState()
            } else {
                MessageList(
                    messages = filteredMessages,
                    riskLevels = riskLevels,
                    onMessageClick = onMessageClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedFilter: RiskLevel?,
    onFilterSelected: (RiskLevel?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
            label = { Text("全部") },
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            selected = selectedFilter == RiskLevel.HIGH,
            onClick = { onFilterSelected(RiskLevel.HIGH) },
            label = { Text("高风险") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.error,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            selected = selectedFilter == RiskLevel.MEDIUM,
            onClick = { onFilterSelected(RiskLevel.MEDIUM) },
            label = { Text("中风险") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            selected = selectedFilter == RiskLevel.LOW,
            onClick = { onFilterSelected(RiskLevel.LOW) },
            label = { Text("低风险") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    riskLevels: Map<String, RiskLevel>,
    onMessageClick: (ChatMessage) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageItem(
                message = message,
                riskLevel = riskLevels[message.id] ?: RiskLevel.NONE,
                onClick = { onMessageClick(message) }
            )
        }
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
    riskLevel: RiskLevel,
    onClick: () -> Unit
) {
    val backgroundColor = when (riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondaryContainer
        RiskLevel.NONE -> MaterialTheme.colorScheme.surface
    }
    
    val icon = when (riskLevel) {
        RiskLevel.HIGH -> Icons.Default.Error
        RiskLevel.MEDIUM -> Icons.Default.Warning
        RiskLevel.LOW -> Icons.Default.CheckCircle
        RiskLevel.NONE -> Icons.Default.CheckCircle
    }
    
    val iconTint = when (riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
        RiskLevel.NONE -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.platform,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            RiskLevelBadge(riskLevel = riskLevel)
        }
    }
}

@Composable
fun RiskLevelBadge(riskLevel: RiskLevel) {
    val (text, color) = when (riskLevel) {
        RiskLevel.HIGH -> "高风险" to MaterialTheme.colorScheme.error
        RiskLevel.MEDIUM -> "中风险" to MaterialTheme.colorScheme.tertiary
        RiskLevel.LOW -> "低风险" to MaterialTheme.colorScheme.secondary
        RiskLevel.NONE -> "无风险" to MaterialTheme.colorScheme.primary
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "暂无风险消息",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "您的聊天记录中没有检测到风险",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MessageDetailDialog(
    message: ChatMessage,
    riskLevel: RiskLevel,
    onDismiss: () -> Unit,
    onStartEvidence: () -> Unit,
    onMarkFalsePositive: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val icon = when (riskLevel) {
                    RiskLevel.HIGH -> Icons.Default.Error
                    RiskLevel.MEDIUM -> Icons.Default.Warning
                    RiskLevel.LOW -> Icons.Default.CheckCircle
                    RiskLevel.NONE -> Icons.Default.CheckCircle
                }
                
                val iconTint = when (riskLevel) {
                    RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                    RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
                    RiskLevel.NONE -> MaterialTheme.colorScheme.primary
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
                
                Text(
                    text = "消息详情",
                    color = iconTint
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow("发送者", message.sender)
                InfoRow("平台", message.platform)
                InfoRow("时间", formatTimestamp(message.timestamp))
                InfoRow("风险等级", getRiskLevelText(riskLevel))
                
                Divider()
                
                Text(
                    text = "消息内容",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStartEvidence()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("开始存证")
            }
        },
        dismissButton = {
            TextButton(onClick = onMarkFalsePositive) {
                Text("标记误报")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getRiskLevelText(riskLevel: RiskLevel): String {
    return when (riskLevel) {
        RiskLevel.HIGH -> "高风险"
        RiskLevel.MEDIUM -> "中风险"
        RiskLevel.LOW -> "低风险"
        RiskLevel.NONE -> "无风险"
    }
}
