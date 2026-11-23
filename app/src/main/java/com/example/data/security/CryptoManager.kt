package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "revisekeep_api_key_master"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    fun encryptApiKeyWithKeystore(plainKey: String): String {
        if (plainKey.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainKey.toByteArray(Charsets.UTF_8))
            "${iv.toHex()}:${cipherBytes.toHex()}"
        } catch (_: Exception) {
            deleteKeystoreKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainKey.toByteArray(Charsets.UTF_8))
            "${iv.toHex()}:${cipherBytes.toHex()}"
        }
    }

    fun decryptApiKeyWithKeystore(blob: String): String {
        if (blob.isBlank() || !blob.contains(":")) return ""
        return runCatching {
            val parts = blob.split(":", limit = 2)
            val iv = parts[0].hexToBytes()
            val cipherBytes = parts[1].hexToBytes()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun deleteKeystoreKey() {
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        }
    }

    fun generateSaltBase64(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.hexToBytes())
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.toHex()
    }

    fun verifyPin(inputPin: String, storedHash: String, saltHex: String): Boolean {
        if (storedHash.isBlank() || saltHex.isBlank()) return false
        return MessageDigest.isEqual(
            hashPin(inputPin, saltHex).toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun encryptBackupWithPassphrase(plainJson: String, passphrase: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveAesKeyFromPassphrase(passphrase, salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        return "REVISEKEEP_ENC_V1:${salt.toHex()}:${iv.toHex()}:${encrypted.toHex()}"
    }

    fun decryptBackupWithPassphrase(payload: String, passphrase: String): String {
        val parts = payload.trim().split(":")
        require(parts.size == 4 && parts[0] == "REVISEKEEP_ENC_V1") { "Invalid backup format" }
        val salt = parts[1].hexToBytes()
        val iv = parts[2].hexToBytes()
        val encrypted = parts[3].hexToBytes()
        val key = deriveAesKeyFromPassphrase(passphrase, salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun encryptForCloudSync(plainText: String, uid: String): String {
        if (plainText.isBlank() || uid.isBlank()) return ""
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveAesKeyFromPassphrase("rk_cloud_v1_$uid", salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return "RK_CLOUD_V1:${salt.toHex()}:${iv.toHex()}:${encrypted.toHex()}"
    }

    fun decryptFromCloudSync(blob: String, uid: String): String {
        if (blob.isBlank() || uid.isBlank()) return ""
        return runCatching {
            val parts = blob.trim().split(":")
            require(parts.size == 4 && parts[0] == "RK_CLOUD_V1") { "Invalid cloud cipher format" }
            val salt = parts[1].hexToBytes()
            val iv = parts[2].hexToBytes()
            val encrypted = parts[3].hexToBytes()
            val key = deriveAesKeyFromPassphrase("rk_cloud_v1_$uid", salt)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun deriveAesKeyFromPassphrase(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        check(length % 2 == 0) { "Invalid hex string" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
