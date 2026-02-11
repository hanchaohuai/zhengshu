package com.zhengshu.utils

import com.zhengshu.data.model.EvidencePackage
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HashUtils {

    fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun calculateSHA256(text: String): String {
        return calculateSHA256(text.toByteArray(Charsets.UTF_8))
    }

    fun calculateSHA256(file: File): String {
        return calculateSHA256(file.readBytes())
    }

    fun calculateHash(evidencePackage: EvidencePackage): String {
        val data = serializeEvidencePackage(evidencePackage)
        return calculateSHA256(data)
    }

    fun calculateMD5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun calculateMD5(text: String): String {
        return calculateMD5(text.toByteArray(Charsets.UTF_8))
    }

    fun calculateMD5(file: File): String {
        return calculateMD5(file.readBytes())
    }

    fun verifyHash(data: ByteArray, expectedHash: String): Boolean {
        val actualHash = calculateSHA256(data)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    fun verifyHash(file: File, expectedHash: String): Boolean {
        val actualHash = calculateSHA256(file)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    private fun serializeEvidencePackage(evidencePackage: EvidencePackage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        
        outputStream.write(evidencePackage.id.toByteArray())
        outputStream.write(evidencePackage.title.toByteArray())
        outputStream.write(evidencePackage.description.toByteArray())
        outputStream.write(evidencePackage.timestamp.toString().toByteArray())
        
        evidencePackage.chatMessages.forEach { message ->
            outputStream.write(message.id.toByteArray())
            outputStream.write(message.content.toByteArray())
        }
        
        return outputStream.toByteArray()
    }

    fun createProofPackage(
        evidencePackage: EvidencePackage,
        outputPath: String
    ): File {
        val outputFile = File(outputPath)
        val zipOutputStream = ZipOutputStream(outputFile.outputStream())
        
        try {
            val manifestEntry = ZipEntry("manifest.json")
            zipOutputStream.putNextEntry(manifestEntry)
            zipOutputStream.write(createManifest(evidencePackage).toByteArray())
            zipOutputStream.closeEntry()
            
            val dataEntry = ZipEntry("data/")
            zipOutputStream.putNextEntry(dataEntry)
            zipOutputStream.closeEntry()
            
            evidencePackage.screenRecordPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val entry = ZipEntry("data/${file.name}")
                    zipOutputStream.putNextEntry(entry)
                    file.inputStream().copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
            
            evidencePackage.screenshotPaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    val entry = ZipEntry("data/${file.name}")
                    zipOutputStream.putNextEntry(entry)
                    file.inputStream().copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
            
            val messagesEntry = ZipEntry("data/messages.json")
            zipOutputStream.putNextEntry(messagesEntry)
            zipOutputStream.write(serializeMessages(evidencePackage.chatMessages).toByteArray())
            zipOutputStream.closeEntry()
            
            val environmentEntry = ZipEntry("data/environment.json")
            zipOutputStream.putNextEntry(environmentEntry)
            zipOutputStream.write(serializeEnvironment(evidencePackage.environmentData).toByteArray())
            zipOutputStream.closeEntry()
            
            val hashEntry = ZipEntry("hash.txt")
            zipOutputStream.putNextEntry(hashEntry)
            zipOutputStream.write(evidencePackage.hashValue.toByteArray())
            zipOutputStream.closeEntry()
            
        } finally {
            zipOutputStream.close()
        }
        
        return outputFile
    }

    private fun createManifest(evidencePackage: EvidencePackage): String {
        return """
            {
                "id": "${evidencePackage.id}",
                "title": "${evidencePackage.title}",
                "description": "${evidencePackage.description}",
                "timestamp": ${evidencePackage.timestamp},
                "hash": "${evidencePackage.hashValue}",
                "blockchain_address": "${evidencePackage.blockchainAddress ?: ""}",
                "screen_record": "${evidencePackage.screenRecordPath ?: ""}",
                "screenshot_count": ${evidencePackage.screenshotPaths.size},
                "message_count": ${evidencePackage.chatMessages.size}
            }
        """.trimIndent()
    }

    private fun serializeMessages(messages: List<com.zhengshu.data.model.ChatMessage>): String {
        val json = StringBuilder("[")
        messages.forEachIndexed { index, message ->
            if (index > 0) json.append(",")
            json.append("""
                {
                    "id": "${message.id}",
                    "sender": "${message.sender}",
                    "content": "${message.content.replace("\"", "\\\"")}",
                    "timestamp": ${message.timestamp},
                    "platform": "${message.platform}"
                }
            """.trimIndent())
        }
        json.append("]")
        return json.toString()
    }

    private fun serializeEnvironment(environment: com.zhengshu.data.model.EnvironmentData): String {
        return """
            {
                "device_model": "${environment.deviceModel}",
                "os_version": "${environment.osVersion}",
                "app_version": "${environment.appVersion}",
                "network_type": "${environment.networkType}",
                "ip_address": "${environment.ipAddress ?: ""}",
                "location": "${environment.location ?: ""}"
            }
        """.trimIndent()
    }
}
