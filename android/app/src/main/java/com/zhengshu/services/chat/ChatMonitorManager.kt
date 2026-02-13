package com.zhengshu.services.chat

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.zhengshu.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChatMonitorManager {
    
    private val _messageFlow = MutableSharedFlow<ChatMessage>()
    val messageFlow = _messageFlow.asSharedFlow()
    
    private var serviceInstance: ChatMonitorService? = null
    
    fun setServiceInstance(service: ChatMonitorService) {
        serviceInstance = service
    }
    
    fun getServiceInstance(): ChatMonitorService? = serviceInstance
    
    suspend fun emitMessage(message: ChatMessage) {
        _messageFlow.emit(message)
    }
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        val packageName = context.packageName
        val serviceName = "${packageName}/com.zhengshu.services.chat.ChatMonitorService"
        
        return !TextUtils.isEmpty(enabledServices) && enabledServices.contains(serviceName)
    }
    
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val componentName = ComponentName(packageName, "com.zhengshu.services.chat.NotificationMonitorService")
        
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        val flattenedName = componentName.flattenToString()
        return !TextUtils.isEmpty(enabledListeners) && enabledListeners.contains(flattenedName)
    }
    
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    fun getMessageHistory(): List<ChatMessage> {
        return serviceInstance?.getMessageHistory() ?: emptyList()
    }
    
    fun clearHistory() {
        serviceInstance?.clearHistory()
    }
}