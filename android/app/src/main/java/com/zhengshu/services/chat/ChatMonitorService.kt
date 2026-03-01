package com.zhengshu.services.chat

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zhengshu.data.model.ChatMessage
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
            "com.tencent.mobileqq"
        )
        
        private const val MAX_HISTORY_SIZE = 100
        private const val MAX_BUFFER_SIZE = 5000
        private const val MAX_TRAVERSE_DEPTH = 20
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private var currentChatPackage: String? = null
    private var lastProcessedContent: String = ""
    private var lastProcessedTime: Long = 0
    private val messageHistory = mutableListOf<ChatMessage>()
    
    private val messageBuffer = StringBuilder()
    private var lastMessageHash: Int = 0
    
    private var processingJob: Job? = null
    private var isServiceRunning = false
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "ChatMonitorService connected")
        ChatMonitorManager.setServiceInstance(this)
        
        isServiceRunning = true
        startBufferProcessing()
    }
    
    private fun startBufferProcessing() {
        processingJob?.cancel()
        processingJob = serviceScope.launch {
            while (isServiceRunning) {
                try {
                    delay(2000)
                    processBufferedMessages()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in buffer processing: ${e.message}", e)
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "ChatMonitorService interrupted")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        serviceScope.launch {
            try {
                val packageName = event.packageName?.toString()
                
                if (packageName == null || !isSupportedPackage(packageName)) {
                    return@launch
                }
                
                currentChatPackage = packageName
                
                when (event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        handleWindowStateChanged(event, packageName)
                    }
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        handleWindowContentChanged(event, packageName)
                    }
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                        handleViewTextChanged(event, packageName)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing accessibility event: ${e.message}", e)
            }
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent, packageName: String) {
        Log.d(TAG, "Window state changed: $packageName")
        
        serviceScope.launch {
            try {
                val nodeInfo = rootInActiveWindow
                if (nodeInfo != null) {
                    val textContent = extractTextFromNode(nodeInfo)
                    if (textContent.isNotEmpty() && textContent.length < 1000) {
                        processNewContent(textContent, packageName, "WindowStateChanged")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling window state changed: ${e.message}", e)
            }
        }
    }
    
    private fun handleWindowContentChanged(event: AccessibilityEvent, packageName: String) {
        serviceScope.launch {
            try {
                val nodeInfo = event.source ?: rootInActiveWindow
                if (nodeInfo != null) {
                    val textContent = extractTextFromNode(nodeInfo)
                    if (textContent.isNotEmpty() && textContent.length < 1000) {
                        processNewContent(textContent, packageName, "ContentChanged")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling window content changed: ${e.message}", e)
            }
        }
    }
    
    private fun handleViewTextChanged(event: AccessibilityEvent, packageName: String) {
        serviceScope.launch {
            try {
                val text = event.text?.joinToString(" ") ?: ""
                if (text.isNotEmpty() && text.length < 500) {
                    processNewContent(text, packageName, "ViewTextChanged")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling view text changed: ${e.message}", e)
            }
        }
    }
    
    private fun processNewContent(content: String, packageName: String, source: String) {
        val currentTime = System.currentTimeMillis()
        
        if (content == lastProcessedContent && (currentTime - lastProcessedTime) < 2000) {
            return
        }
        
        val contentHash = content.hashCode()
        if (contentHash == lastMessageHash && (currentTime - lastProcessedTime) < 1000) {
            return
        }
        
        lastProcessedContent = content
        lastProcessedTime = currentTime
        lastMessageHash = contentHash
        
        Log.d(TAG, "New content from $source: ${content.take(50)}")
        
        if (isLikelyChatMessage(content)) {
            if (messageBuffer.length < MAX_BUFFER_SIZE) {
                messageBuffer.append(content).append("\n")
                Log.d(TAG, "Added to buffer, buffer size: ${messageBuffer.length}")
            } else {
                Log.w(TAG, "Buffer full (${messageBuffer.length}), skipping content")
            }
        }
    }
    
    private fun extractTextFromNode(node: AccessibilityNodeInfo): String {
        val result = StringBuilder()
        traverseNode(node, result, 0, MAX_TRAVERSE_DEPTH)
        return result.toString().trim()
    }
    
    private fun traverseNode(node: AccessibilityNodeInfo, text: StringBuilder, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) {
            return
        }
        
        try {
            node.text?.toString()?.let { nodeText ->
                if (nodeText.length > 1) {
                    text.append(nodeText).append(" ")
                }
            }
            
            node.contentDescription?.toString()?.let { descText ->
                if (descText.length > 1) {
                    text.append(descText).append(" ")
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    traverseNode(child, text, depth + 1, maxDepth)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing node at depth $depth: ${e.message}")
        }
    }
    
    private fun isLikelyChatMessage(text: String): Boolean {
        if (text.length < 3) {
            return false
        }
        
        val messageIndicators = listOf(
            "：", ":", "说", "发", "发送", "图片", "语音", "视频",
            "转账", "汇款", "投资", "理财", "验证码", "中奖", "退款",
            "兼职", "刷单", "你好", "在吗", "请", "帮", "可以", "需要",
            "钱", "元", "¥", "￥", "银行", "卡", "账号", "密码",
            "链接", "点击", "下载", "安装"
        )
        
        if (messageIndicators.any { text.contains(it) }) {
            return true
        }
        
        if (text.length > 10 && Regex(""".*[\u4e00-\u9fa5]+.*""").matches(text)) {
            return true
        }
        
        return false
    }
    
    private fun processBufferedMessages() {
        val bufferedText = messageBuffer.toString().trim()
        if (bufferedText.isEmpty()) {
            return
        }
        
        Log.d(TAG, "Processing buffered messages: ${bufferedText.take(100)}")
        
        val chatMessage = createChatMessage(bufferedText)
        
        synchronized(messageHistory) {
            messageHistory.add(chatMessage)
            if (messageHistory.size > MAX_HISTORY_SIZE) {
                messageHistory.removeAt(0)
                Log.d(TAG, "Removed old message, history size: ${messageHistory.size}")
            }
        }
        
        Log.d(TAG, "Created ChatMessage: id=${chatMessage.id}, sender=${chatMessage.sender}, content=${chatMessage.content.take(50)}")
        
        serviceScope.launch {
            try {
                ChatMonitorManager.emitMessage(chatMessage)
                Log.d(TAG, "Message emitted successfully to ChatMonitorManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error emitting message: ${e.message}", e)
            }
        }
        
        messageBuffer.clear()
    }
    
    private fun createChatMessage(content: String): ChatMessage {
        val platform = when (currentChatPackage) {
            "com.tencent.mm" -> "微信"
            "com.tencent.mobileqq" -> "QQ"
            else -> "未知平台"
        }
        
        val sender = extractSender(content)
        
        return ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = sender,
            content = content,
            timestamp = System.currentTimeMillis(),
            platform = platform,
            isRevoked = false,
            isDeleted = false
        )
    }
    
    private fun extractSender(content: String): String {
        val lines = content.split("\n")
        for (line in lines) {
            if (line.contains("：") || line.contains(":")) {
                val parts = line.split(Regex("[：:]"))
                if (parts.size >= 2 && parts[0].length > 0 && parts[0].length < 20) {
                    return parts[0].trim()
                }
            }
        }
        
        val firstLine = lines.firstOrNull { it.isNotEmpty() } ?: ""
        if (firstLine.length > 0 && firstLine.length < 20) {
            return firstLine.trim()
        }
        
        return "未知发送者"
    }
    
    private fun isSupportedPackage(packageName: String): Boolean {
        return supportedPackages.contains(packageName)
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
        isServiceRunning = false
        processingJob?.cancel()
        messageBuffer.clear()
        messageHistory.clear()
        serviceScope.cancel()
    }
}