package com.zhengshu.services.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zhengshu.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DataCollectionService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _chatMessageFlow = MutableSharedFlow<ChatMessage>()
    val chatMessageFlow = _chatMessageFlow.asSharedFlow()
    
    private val _textContentFlow = MutableSharedFlow<String>()
    val textContentFlow = _textContentFlow.asSharedFlow()
    
    private val _behaviorDataFlow = MutableSharedFlow<BehaviorData>()
    val behaviorDataFlow = _behaviorDataFlow.asSharedFlow()
    
    private var lastClickTime = 0L
    private var clickCount = 0
    private var inputStartTime = 0L
    private var inputCharCount = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleClickEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event)
            }
        }
    }

    override fun onInterrupt() {
        
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        val text = event.text?.toString() ?: return
        if (text.isBlank()) return
        
        inputStartTime = System.currentTimeMillis()
        inputCharCount = text.length
        
        val packageName = event.packageName?.toString() ?: "unknown"
        val className = event.className?.toString() ?: ""
        
        serviceScope.launch {
            _textContentFlow.emit(text)
            
            if (isChatApp(packageName) && isMessageField(className)) {
                val chatMessage = ChatMessage(
                    id = generateMessageId(),
                    sender = extractSender(event.source),
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    platform = packageName
                )
                _chatMessageFlow.emit(chatMessage)
            }
        }
    }

    private fun handleClickEvent(event: AccessibilityEvent) {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastClickTime < 1000) {
            clickCount++
        } else {
            clickCount = 1
        }
        
        lastClickTime = currentTime
        
        val packageName = event.packageName?.toString() ?: "unknown"
        val className = event.className?.toString() ?: ""
        
        serviceScope.launch {
            val behaviorData = BehaviorData(
                type = BehaviorType.CLICK,
                packageName = packageName,
                className = className,
                timestamp = currentTime,
                clickFrequency = clickCount
            )
            _behaviorDataFlow.emit(behaviorData)
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        if (isChatApp(packageName)) {
            val rootNode = rootInActiveWindow
            rootNode?.let { extractChatMessages(it, packageName) }
        }
    }

    private fun extractChatMessages(nodeInfo: AccessibilityNodeInfo, packageName: String) {
        val messages = mutableListOf<ChatMessage>()
        
        fun traverseNode(node: AccessibilityNodeInfo) {
            val text = node.text?.toString()
            val contentDescription = node.contentDescription?.toString()
            
            if (!text.isNullOrBlank() || !contentDescription.isNullOrBlank()) {
                val content = text ?: contentDescription ?: ""
                
                if (content.length > 5 && content.length < 1000) {
                    messages.add(
                        ChatMessage(
                            id = generateMessageId(),
                            sender = extractSender(node),
                            content = content,
                            timestamp = System.currentTimeMillis(),
                            platform = packageName
                        )
                    )
                }
            }
            
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverseNode(it) }
            }
        }
        
        traverseNode(nodeInfo)
        
        serviceScope.launch {
            messages.forEach { _chatMessageFlow.emit(it) }
        }
    }

    private fun isChatApp(packageName: String): Boolean {
        val chatApps = listOf(
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.alibaba.android.rimet",
            "com.ss.android.ugc.aweme",
            "com.smile.gifmaker"
        )
        return chatApps.any { packageName.contains(it) }
    }

    private fun isMessageField(className: String): Boolean {
        return className.contains("EditText") || 
               className.contains("TextView") ||
               className.contains("Input")
    }

    private fun extractSender(nodeInfo: AccessibilityNodeInfo?): String {
        var parent = nodeInfo?.parent
        var depth = 0
        
        while (parent != null && depth < 5) {
            val text = parent.text?.toString()
            val contentDescription = parent.contentDescription?.toString()
            
            if (!text.isNullOrBlank() && text.length < 50) {
                return text
            }
            if (!contentDescription.isNullOrBlank() && contentDescription.length < 50) {
                return contentDescription
            }
            
            parent = parent.parent
            depth++
        }
        
        return "unknown"
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

data class BehaviorData(
    val type: BehaviorType,
    val packageName: String,
    val className: String,
    val timestamp: Long,
    val clickFrequency: Int = 0,
    val inputSpeed: Int = 0
)

enum class BehaviorType {
    CLICK,
    INPUT,
    SCROLL,
    NAVIGATION
}
