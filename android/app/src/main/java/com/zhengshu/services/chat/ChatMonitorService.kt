package com.zhengshu.services.chat

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.services.ai.DataCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "ChatMonitorService"
        
        val supportedPackages = listOf(
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.tencent.wework",
            "com.alibaba.android.rimet",
            "com.alibaba.android.dingtalk"
        )
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var dataCollector: DataCollector? = null
    
    private var currentChatPackage: String? = null
    private var lastMessageContent: String = ""
    private var messageBuffer = StringBuilder()
    private var lastWindowContent: String = ""
    
    private val messageHistory = mutableListOf<ChatMessage>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "ChatMonitorService connected")
        ChatMonitorManager.setServiceInstance(this)
        dataCollector = DataCollector(this)
        dataCollector?.startCollection()
        
        serviceScope.launch {
            while (true) {
                delay(5000)
                analyzeBufferedMessages()
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "ChatMonitorService interrupted")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "Received accessibility event: ${event.eventType}, package: ${event.packageName}")
        
        val packageName = event.packageName?.toString() ?: return
        if (!isSupportedPackage(packageName)) {
            Log.d(TAG, "Package not supported: $packageName")
            return
        }
        
        currentChatPackage = packageName
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Window state changed for package: $packageName")
            val nodeInfo = rootInActiveWindow
            if (nodeInfo != null) {
                Log.d(TAG, "rootInActiveWindow is not null")
                val textContent = extractTextFromNode(nodeInfo)
                Log.d(TAG, "Window content: ${textContent.take(100)}")
                
                if (textContent.isNotEmpty() && textContent != lastWindowContent) {
                    lastWindowContent = textContent
                    
                    if (isLikelyNewMessage(textContent)) {
                        messageBuffer.append(textContent).append("\n")
                        Log.d(TAG, "Added to message buffer")
                    }
                }
            } else {
                Log.d(TAG, "rootInActiveWindow is null")
            }
            return
        }
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val nodeInfo = event.source ?: rootInActiveWindow
            if (nodeInfo != null) {
                val textContent = extractTextFromNode(nodeInfo)
                Log.d(TAG, "Content changed: ${textContent.take(50)}")
                
                if (textContent.isNotEmpty() && textContent != lastMessageContent) {
                    lastMessageContent = textContent
                    
                    if (isLikelyNewMessage(textContent)) {
                        messageBuffer.append(textContent).append("\n")
                        Log.d(TAG, "Added to message buffer")
                    }
                }
            } else {
                Log.d(TAG, "event.source and rootInActiveWindow are both null")
            }
        }
    }
    
    private fun extractTextFromNode(node: AccessibilityNodeInfo): String {
        val text = StringBuilder()
        traverseNode(node, text)
        val result = text.toString().trim()
        Log.d(TAG, "extractTextFromNode result: ${result.take(100)}")
        return result
    }
    
    private fun traverseNode(node: AccessibilityNodeInfo, text: StringBuilder) {
        if (node.text != null && node.text.isNotEmpty()) {
            val nodeText = node.text.toString()
            if (nodeText.length > 1) {
                text.append(nodeText).append(" ")
            }
        }
        
        if (node.contentDescription != null && node.contentDescription.isNotEmpty()) {
            val descText = node.contentDescription.toString()
            if (descText.length > 1) {
                text.append(descText).append(" ")
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    traverseNode(child, text)
                } catch (e: Exception) {
                    Log.e(TAG, "Error traversing child node: ${e.message}")
                }
            }
        }
    }
    
    private fun isSupportedPackage(packageName: String): Boolean {
        return true
    }
    
    private fun isLikelyNewMessage(text: String): Boolean {
        if (text.length < 3) {
            Log.d(TAG, "Text too short: ${text.length}")
            return false
        }
        
        val messageIndicators = listOf(
            "：",
            ":",
            "说",
            "发",
            "发送",
            "图片",
            "语音",
            "视频",
            "转账",
            "汇款",
            "投资",
            "理财",
            "验证码",
            "中奖",
            "退款",
            "兼职",
            "刷单",
            "你好",
            "在吗",
            "请",
            "帮",
            "可以",
            "需要",
            "钱",
            "元",
            "¥",
            "￥",
            "￥",
            "银行",
            "卡",
            "账号",
            "密码",
            "链接",
            "点击",
            "下载",
            "安装"
        )
        
        val hasIndicator = messageIndicators.any { text.contains(it) }
        Log.d(TAG, "isLikelyNewMessage: hasIndicator=$hasIndicator, textLength=${text.length}")
        
        if (hasIndicator) {
            return true
        }
        
        if (text.length > 10) {
            Log.d(TAG, "Text is long enough (${text.length}), treating as potential message")
            return true
        }
        
        return false
    }
    
    private fun analyzeBufferedMessages() {
        val bufferedText = messageBuffer.toString()
        if (bufferedText.isNotEmpty()) {
            Log.d(TAG, "Analyzing buffered messages: $bufferedText")
            val chatMessage = createChatMessage(bufferedText)
            messageHistory.add(chatMessage)
            
            serviceScope.launch {
                dataCollector?.collectChatMessage(chatMessage)
                ChatMonitorManager.emitMessage(chatMessage)
            }
            
            messageBuffer.clear()
        }
    }
    
    private fun createChatMessage(content: String): ChatMessage {
        val platform = when (currentChatPackage) {
            "com.tencent.mm" -> "微信"
            "com.tencent.mobileqq" -> "QQ"
            "com.tencent.wework" -> "企业微信"
            "com.alibaba.android.rimet" -> "钉钉"
            "com.alibaba.android.dingtalk" -> "钉钉"
            else -> "未知平台"
        }
        
        return ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = extractSender(content),
            content = content,
            timestamp = System.currentTimeMillis(),
            platform = platform,
            isRevoked = false,
            isDeleted = false
        )
    }
    
    private fun extractSender(content: String): String {
        val senderPatterns = listOf(
            Regex("^(.+?)[：:](.+)"),
            Regex("^(.+?)[:](.+)"),
            Regex("^(.+?)说")
        )
        
        for (pattern in senderPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return "未知发送者"
    }
    
    fun getMessageHistory(): List<ChatMessage> {
        return messageHistory.toList()
    }
    
    fun clearHistory() {
        messageHistory.clear()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ChatMonitorService destroyed")
        serviceScope.cancel()
        dataCollector?.stopCollection()
    }
}