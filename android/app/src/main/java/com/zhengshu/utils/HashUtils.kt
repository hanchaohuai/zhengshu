package com.zhengshu.utils

import com.zhengshu.data.model.EvidencePackage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HashUtils {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun calculateSHA256(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw HashException("Failed to calculate SHA-256 hash", e)
        }
    }

    fun calculateSHA256(text: String): String {
        return calculateSHA256(text.toByteArray(Charsets.UTF_8))
    }

    fun calculateSHA256(file: File): String {
        if (!file.exists()) {
            throw HashException("File does not exist: ${file.absolutePath}")
        }
        if (!file.canRead()) {
            throw HashException("Cannot read file: ${file.absolutePath}")
        }
        return try {
            calculateSHA256(file.readBytes())
        } catch (e: HashException) {
            throw e
        } catch (e: Exception) {
            throw HashException("Failed to calculate SHA-256 hash for file", e)
        }
    }

    fun calculateHash(evidencePackage: EvidencePackage): String {
        return try {
            val data = serializeEvidencePackage(evidencePackage)
            calculateSHA256(data)
        } catch (e: HashException) {
            throw e
        } catch (e: Exception) {
            throw HashException("Failed to calculate hash for evidence package", e)
        }
    }

    fun calculateMD5(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(data)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw HashException("Failed to calculate MD5 hash", e)
        }
    }

    fun calculateMD5(text: String): String {
        return calculateMD5(text.toByteArray(Charsets.UTF_8))
    }

    fun calculateMD5(file: File): String {
        if (!file.exists()) {
            throw HashException("File does not exist: ${file.absolutePath}")
        }
        if (!file.canRead()) {
            throw HashException("Cannot read file: ${file.absolutePath}")
        }
        return try {
            calculateMD5(file.readBytes())
        } catch (e: HashException) {
            throw e
        } catch (e: Exception) {
            throw HashException("Failed to calculate MD5 hash for file", e)
        }
    }

    fun verifyHash(data: ByteArray, expectedHash: String): Boolean {
        return try {
            val actualHash = calculateSHA256(data)
            actualHash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun verifyHash(file: File, expectedHash: String): Boolean {
        return try {
            val actualHash = calculateSHA256(file)
            actualHash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun serializeEvidencePackage(evidencePackage: EvidencePackage): ByteArray {
        return try {
            val outputStream = ByteArrayOutputStream()
            
            outputStream.write(evidencePackage.id.toByteArray())
            outputStream.write(evidencePackage.title.toByteArray())
            outputStream.write(evidencePackage.description.toByteArray())
            outputStream.write(evidencePackage.timestamp.toString().toByteArray())
            
            evidencePackage.chatMessages.forEach { message ->
                outputStream.write(message.id.toByteArray())
                outputStream.write(message.content.toByteArray())
            }
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            throw HashException("Failed to serialize evidence package", e)
        }
    }

    fun createProofPackage(
        evidencePackage: EvidencePackage,
        outputPath: String
    ): File {
        if (outputPath.isEmpty()) {
            throw HashException("Output path cannot be empty")
        }
        
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        
        return try {
            ZipOutputStream(outputFile.outputStream()).use { zipOutputStream ->
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
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOutputStream)
                        }
                        zipOutputStream.closeEntry()
                    }
                }
                
                evidencePackage.screenshotPaths.forEach { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val entry = ZipEntry("data/${file.name}")
                        zipOutputStream.putNextEntry(entry)
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOutputStream)
                        }
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
            }
            
            outputFile
        } catch (e: HashException) {
            throw e
        } catch (e: Exception) {
            throw HashException("Failed to create proof package", e)
        }
    }

    private fun createManifest(evidencePackage: EvidencePackage): String {
        return try {
            json.encodeToString(
                mapOf(
                    "id" to evidencePackage.id,
                    "title" to evidencePackage.title,
                    "description" to evidencePackage.description,
                    "timestamp" to evidencePackage.timestamp,
                    "hash" to evidencePackage.hashValue,
                    "blockchain_address" to (evidencePackage.blockchainAddress ?: ""),
                    "screen_record" to (evidencePackage.screenRecordPath ?: ""),
                    "screenshot_count" to evidencePackage.screenshotPaths.size,
                    "message_count" to evidencePackage.chatMessages.size
                )
            )
        } catch (e: Exception) {
            throw HashException("Failed to create manifest", e)
        }
    }

    private fun serializeMessages(messages: List<com.zhengshu.data.model.ChatMessage>): String {
        return try {
            json.encodeToString(messages)
        } catch (e: Exception) {
            throw HashException("Failed to serialize messages", e)
        }
    }

    private fun serializeEnvironment(environment: com.zhengshu.data.model.EnvironmentData): String {
        return try {
            json.encodeToString(environment)
        } catch (e: Exception) {
            throw HashException("Failed to serialize environment data", e)
        }
    }

    class HashException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
