package com.zhengshu.services.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.zhengshu.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _smsMessageFlow = MutableSharedFlow<ChatMessage>()
    val smsMessageFlow = _smsMessageFlow.asSharedFlow()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            messages.forEach { smsMessage ->
                val sender = smsMessage.originatingAddress ?: "unknown"
                val content = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis
                
                val chatMessage = ChatMessage(
                    id = "sms_${timestamp}_${sender.hashCode()}",
                    sender = sender,
                    content = content,
                    timestamp = timestamp,
                    platform = "sms"
                )
                
                receiverScope.launch {
                    _smsMessageFlow.emit(chatMessage)
                }
            }
        }
    }
}
