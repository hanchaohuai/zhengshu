package com.zhengshu.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionManager(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "zhengshu_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SHARED_PREFS_NAME = "zhengshu_encrypted_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
    }

    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(context)
                .setKeyGenParameterSpec(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(false)
                        .build()
                )
                .build()
        } catch (e: Exception) {
            throw EncryptionException("Failed to create master key", e)
        }
    }

    private val encryptedSharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SHARED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw EncryptionException("Failed to create encrypted shared preferences", e)
        }
    }

    fun encryptString(plaintext: String): String {
        if (plaintext.isEmpty()) {
            throw EncryptionException("Plaintext cannot be empty")
        }
        
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedBytes
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt string", e)
        }
    }

    fun decryptString(ciphertext: String): String {
        if (ciphertext.isEmpty()) {
            throw EncryptionException("Ciphertext cannot be empty")
        }
        
        return try {
            val combined = android.util.Base64.decode(ciphertext, android.util.Base64.NO_WRAP)

            if (combined.size < GCM_IV_LENGTH) {
                throw EncryptionException("Invalid ciphertext: too short")
            }

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: EncryptionException) {
            throw e
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt string", e)
        }
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        if (!inputFile.exists()) {
            throw EncryptionException("Input file does not exist: ${inputFile.absolutePath}")
        }
        
        if (!inputFile.canRead()) {
            throw EncryptionException("Cannot read input file: ${inputFile.absolutePath}")
        }
        
        try {
            val encryptedFile = EncryptedFile.Builder(
                outputFile,
                context,
                KEY_ALIAS,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            inputFile.inputStream().use { inputStream ->
                encryptedFile.openFileOutput().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: EncryptionException) {
            throw e
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt file", e)
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        if (!inputFile.exists()) {
            throw EncryptionException("Input file does not exist: ${inputFile.absolutePath}")
        }
        
        if (!inputFile.canRead()) {
            throw EncryptionException("Cannot read input file: ${inputFile.absolutePath}")
        }
        
        try {
            val encryptedFile = EncryptedFile.Builder(
                inputFile,
                context,
                KEY_ALIAS,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileInput().use { inputStream ->
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: EncryptionException) {
            throw e
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt file", e)
        }
    }

    fun saveEncrypted(key: String, value: String) {
        if (key.isEmpty()) {
            throw EncryptionException("Key cannot be empty")
        }
        
        try {
            encryptedSharedPreferences.edit().putString(key, value).apply()
        } catch (e: Exception) {
            throw EncryptionException("Failed to save encrypted value", e)
        }
    }

    fun getDecrypted(key: String): String? {
        if (key.isEmpty()) {
            throw EncryptionException("Key cannot be empty")
        }
        
        return try {
            encryptedSharedPreferences.getString(key, null)
        } catch (e: Exception) {
            throw EncryptionException("Failed to get decrypted value", e)
        }
    }

    fun removeEncrypted(key: String) {
        if (key.isEmpty()) {
            throw EncryptionException("Key cannot be empty")
        }
        
        try {
            encryptedSharedPreferences.edit().remove(key).apply()
        } catch (e: Exception) {
            throw EncryptionException("Failed to remove encrypted value", e)
        }
    }

    fun clearAllEncrypted() {
        try {
            encryptedSharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            throw EncryptionException("Failed to clear all encrypted values", e)
        }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existingKey != null) {
                return existingKey
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            keyGenerator.generateKey()
        } catch (e: Exception) {
            throw EncryptionException("Failed to get or generate secret key", e)
        }
    }

    class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
}