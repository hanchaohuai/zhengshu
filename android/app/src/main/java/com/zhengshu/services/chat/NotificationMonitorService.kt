package com.zhengshu.services.chat

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.zhengshu.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationMonitorService"
        
        private val supportedPackages = listOf(
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.tencent.wework",
            "com.alibaba.android.rimet",
            "com.alibaba.android.dingtalk"
        )
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationMonitorService created")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        
        if (!isSupportedPackage(packageName)) {
            return
        }
        
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        val content = if (bigText.isNotEmpty()) bigText else text
        
        if (content.isEmpty()) {
            return
        }
        
        Log.d(TAG, "Notification received: package=$packageName, title=$title, content=$content")
        
        val chatMessage = createChatMessage(packageName, title, content)
        
        serviceScope.launch {
            ChatMonitorManager.emitMessage(chatMessage)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        
        if (!isSupportedPackage(packageName)) {
            return
        }
        
        Log.d(TAG, "Notification removed: package=$packageName")
    }
    
    private fun isSupportedPackage(packageName: String): Boolean {
        return supportedPackages.contains(packageName)
    }
    
    private fun createChatMessage(packageName: String, title: String, content: String): ChatMessage {
        val platform = when (packageName) {
            "com.tencent.mm" -> "微信"
            "com.tencent.mobileqq" -> "QQ"
            "com.tencent.wework" -> "企业微信"
            "com.alibaba.android.rimet" -> "钉钉"
            "com.alibaba.android.dingtalk" -> "钉钉"
            else -> "未知平台"
        }
        
        return ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = title,
            content = content,
            timestamp = System.currentTimeMillis(),
            platform = platform,
            isRevoked = false,
            isDeleted = false
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NotificationMonitorService destroyed")
        serviceJob.cancel()
    }
}