package com.zhengshu.services.evidence

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.zhengshu.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatMessageExtractor(private val context: Context) {
    
    suspend fun extractSmsMessages(limit: Int = 100): List<ChatMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<ChatMessage>()
        
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )
        
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"
        
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val address = it.getString(addressIndex) ?: "unknown"
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)
                
                val sender = if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    address
                } else {
                    "我"
                }
                
                messages.add(
                    ChatMessage(
                        id = "sms_$id",
                        sender = sender,
                        content = body,
                        timestamp = date,
                        platform = "sms"
                    )
                )
            }
        }
        
        messages
    }
    
    suspend fun extractWeChatMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        emptyList()
    }
    
    suspend fun extractQQMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        emptyList()
    }
    
    suspend fun extractDingTalkMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        emptyList()
    }
    
    suspend fun extractRevokedMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        emptyList()
    }
    
    suspend fun extractDeletedMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        emptyList()
    }
    
    suspend fun extractMessagesByPlatform(platform: String, limit: Int = 100): List<ChatMessage> {
        return when (platform.lowercase()) {
            "sms" -> extractSmsMessages(limit)
            "wechat", "com.tencent.mm" -> extractWeChatMessages()
            "qq", "com.tencent.mobileqq" -> extractQQMessages()
            "dingtalk", "com.alibaba.android.rimet" -> extractDingTalkMessages()
            else -> emptyList()
        }
    }
    
    suspend fun searchMessages(keyword: String, limit: Int = 50): List<ChatMessage> = withContext(Dispatchers.IO) {
        val allMessages = extractSmsMessages(500)
        
        allMessages.filter { message ->
            message.content.contains(keyword, ignoreCase = true) ||
            message.sender.contains(keyword, ignoreCase = true)
        }.take(limit)
    }
    
    suspend fun getMessagesByDateRange(
        startTime: Long,
        endTime: Long
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        val allMessages = extractSmsMessages(1000)
        
        allMessages.filter { message ->
            message.timestamp in startTime..endTime
        }
    }
    
    suspend fun getMessagesBySender(sender: String, limit: Int = 100): List<ChatMessage> = withContext(Dispatchers.IO) {
        val allMessages = extractSmsMessages(1000)
        
        allMessages.filter { message ->
            message.sender.contains(sender, ignoreCase = true)
        }.take(limit)
    }
}
