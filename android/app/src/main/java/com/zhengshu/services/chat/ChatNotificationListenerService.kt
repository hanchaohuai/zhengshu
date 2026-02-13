package com.zhengshu.services.chat

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.zhengshu.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "ChatNotificationListener"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val supportedPackages = listOf(
        "com.tencent.mm",
        "com.tencent.mobileqq"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        
        serviceScope.launch {
            while (true) {
                try {
                    val notifications = getActiveNotifications()
                    Log.d(TAG, "Active notifications count: ${notifications.size}")
                    
                    notifications.forEach { notification ->
                        processNotification(notification)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notifications: ${e.message}", e)
                }
                delay(5000)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        Log.d(TAG, "Notification posted: ${sbn.packageName}")
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        
        if (!supportedPackages.contains(packageName)) {
            return
        }
        
        Log.d(TAG, "Processing notification from: $packageName")
        
        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            
            val content = listOf(title, text, bigText).joinToString(" ").trim()
            
            if (content.isNotEmpty() && content.length > 3) {
                Log.d(TAG, "Notification content: $content")
                
                val platform = when (packageName) {
                    "com.tencent.mm" -> "微信"
                    "com.tencent.mobileqq" -> "QQ"
                    else -> "未知"
                }
                
                val chatMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    sender = extractSender(content),
                    content = content,
                    platform = platform,
                    timestamp = System.currentTimeMillis()
                )
                
                Log.d(TAG, "Created ChatMessage: id=${chatMessage.id}, sender=${chatMessage.sender}, platform=${chatMessage.platform}")
                
                serviceScope.launch {
                    ChatMonitorManager.emitMessage(chatMessage)
                    Log.d(TAG, "Message emitted successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification: ${e.message}", e)
        }
    }

    private fun extractSender(content: String): String {
        val patterns = listOf(
            Regex("来自\\s*[:：]?\\s*([^\\s]+)"),
            Regex("([^\\s]+)\\s*[:：]"),
            Regex("([^\\s]+)\\s*说")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return "未知"
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Notification listener destroyed")
        serviceScope.cancel()
    }
}