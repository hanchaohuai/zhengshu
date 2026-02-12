package com.zhengshu.services.chat

import android.accessibilityservice.AccessibilityEvent
import android.accessibilityservice.AccessibilityNodeInfo
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.services.ai.DataCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    
    private val messageHistory = mutableListOf<ChatMessage>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        dataCollector = DataCollector(this)
        dataCollector?.startCollection()
        
        serviceScope.launch {
            while (true) {
                delay(5000)
                analyzeBufferedMessages()
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: return
        if (!isSupportedPackage(packageName)) return
        
        currentChatPackage = packageName
        
        val nodeInfo = event.source ?: return
        val textContent = extractTextFromNode(nodeInfo)
        
        if (textContent.isNotEmpty() && textContent != lastMessageContent) {
            lastMessageContent = textContent
            
            if (isLikelyNewMessage(textContent)) {
                messageBuffer.append(textContent).append("\n")
            }
        }
    }
    
    private fun extractTextFromNode(node: AccessibilityNodeInfo): String {
        val text = StringBuilder()
        traverseNode(node, text)
        return text.toString()
    }
    
    private fun traverseNode(node: AccessibilityNodeInfo, text: StringBuilder) {
        if (node.text != null && node.text.isNotEmpty()) {
            text.append(node.text).append(" ")
        }
        
        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), text)
        }
    }
    
    private fun isSupportedPackage(packageName: String): Boolean {
        return supportedPackages.any { packageName.contains(it) }
    }
    
    private fun isLikelyNewMessage(text: String): Boolean {
        val messageIndicators = listOf(
            "：",
            ":",
            "说",
            "发",
            "发送",
            "图片",
            "语音",
            "视频"
        )
        
        return messageIndicators.any { text.contains(it) }
    }
    
    private fun analyzeBufferedMessages() {
        val bufferedText = messageBuffer.toString()
        if (bufferedText.isNotEmpty()) {
            val chatMessage = createChatMessage(bufferedText)
            messageHistory.add(chatMessage)
            
            dataCollector?.collectChatMessage(chatMessage)
            
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
        serviceScope.cancel()
        dataCollector?.stopCollection()
    }
}
