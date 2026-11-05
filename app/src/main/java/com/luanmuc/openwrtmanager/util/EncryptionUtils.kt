package com.luanmuc.openwrtmanager.util
import com.luanmuc.openwrtmanager.util.LogUtils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 加密工具类
 * 提供常用的加密和解密功能
 */
object EncryptionUtils {
    
    private const val TAG = "EncryptionUtils"
    
    // AES相关
    private const val AES_ALGORITHM = "AES/CBC/PKCS7Padding"
    private const val AES_KEY_SIZE = 256
    private const val IV_SIZE = 16
    
    /**
     * MD5加密
     */
    fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            LogUtils.e(TAG, "MD5加密失败", e)
            ""
        }
    }
    
    /**
     * SHA-256加密
     */
    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            LogUtils.e(TAG, "SHA-256加密失败", e)
            ""
        }
    }
    
    /**
     * SHA-1加密
     */
    fun sha1(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            LogUtils.e(TAG, "SHA-1加密失败", e)
            ""
        }
    }
    
    /**
     * Base64编码
     */
    fun base64Encode(input: String): String {
        return try {
            Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Base64编码失败", e)
            ""
        }
    }
    
    /**
     * Base64解码
     */
    fun base64Decode(input: String): String {
        return try {
            val decoded = Base64.decode(input, Base64.NO_WRAP)
            String(decoded)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Base64解码失败", e)
            ""
        }
    }
    
    /**
     * Base64编码（字节数组）
     */
    fun base64EncodeBytes(input: ByteArray): String {
        return try {
            Base64.encodeToString(input, Base64.NO_WRAP)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Base64编码失败", e)
            ""
        }
    }
    
    /**
     * Base64解码（字节数组）
     */
    fun base64DecodeBytes(input: String): ByteArray? {
        return try {
            Base64.decode(input, Base64.NO_WRAP)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Base64解码失败", e)
            null
        }
    }
    
    /**
     * 生成随机密钥
     */
    fun generateRandomKey(length: Int = 32): String {
        return try {
            val random = SecureRandom()
            val key = ByteArray(length)
            random.nextBytes(key)
            base64EncodeBytes(key)
        } catch (e: Exception) {
            LogUtils.e(TAG, "生成随机密钥失败", e)
            ""
        }
    }
    
    /**
     * 生成随机IV
     */
    fun generateRandomIv(): String {
        return try {
            val random = SecureRandom()
            val iv = ByteArray(IV_SIZE)
            random.nextBytes(iv)
            base64EncodeBytes(iv)
        } catch (e: Exception) {
            LogUtils.e(TAG, "生成随机IV失败", e)
            ""
        }
    }
    
    /**
     * AES加密
     */
    fun aesEncrypt(plainText: String, key: String, iv: String): String {
        return try {
            val keyBytes = base64DecodeBytes(key) ?: return ""
            val ivBytes = base64DecodeBytes(iv) ?: return ""
            
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            
            val encrypted = cipher.doFinal(plainText.toByteArray())
            base64EncodeBytes(encrypted)
        } catch (e: Exception) {
            LogUtils.e(TAG, "AES加密失败", e)
            ""
        }
    }
    
    /**
     * AES解密
     */
    fun aesDecrypt(cipherText: String, key: String, iv: String): String {
        return try {
            val keyBytes = base64DecodeBytes(key) ?: return ""
            val ivBytes = base64DecodeBytes(iv) ?: return ""
            val cipherBytes = base64DecodeBytes(cipherText) ?: return ""
            
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decrypted = cipher.doFinal(cipherBytes)
            String(decrypted)
        } catch (e: Exception) {
            LogUtils.e(TAG, "AES解密失败", e)
            ""
        }
    }
    
    /**
     * AES加密（使用密码派生密钥）
     */
    fun aesEncryptWithPassword(plainText: String, password: String): String {
        return try {
            // 使用密码的SHA-256作为密钥
            val key = sha256(password).take(32)
            val iv = sha256(password + "iv").take(16)
            
            val keyBytes = key.toByteArray()
            val ivBytes = iv.toByteArray()
            
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            
            val encrypted = cipher.doFinal(plainText.toByteArray())
            base64EncodeBytes(encrypted)
        } catch (e: Exception) {
            LogUtils.e(TAG, "AES加密失败", e)
            ""
        }
    }
    
    /**
     * AES解密（使用密码派生密钥）
     */
    fun aesDecryptWithPassword(cipherText: String, password: String): String {
        return try {
            // 使用密码的SHA-256作为密钥
            val key = sha256(password).take(32)
            val iv = sha256(password + "iv").take(16)
            
            val keyBytes = key.toByteArray()
            val ivBytes = iv.toByteArray()
            val cipherBytes = base64DecodeBytes(cipherText) ?: return ""
            
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decrypted = cipher.doFinal(cipherBytes)
            String(decrypted)
        } catch (e: Exception) {
            LogUtils.e(TAG, "AES解密失败", e)
            ""
        }
    }
    
    /**
     * 简单的XOR加密（用于简单混淆）
     */
    fun xorEncrypt(input: String, key: String): String {
        return try {
            val inputBytes = input.toByteArray()
            val keyBytes = key.toByteArray()
            val output = ByteArray(inputBytes.size)
            
            for (i in inputBytes.indices) {
                output[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            
            base64EncodeBytes(output)
        } catch (e: Exception) {
            LogUtils.e(TAG, "XOR加密失败", e)
            ""
        }
    }
    
    /**
     * 简单的XOR解密
     */
    fun xorDecrypt(input: String, key: String): String {
        return try {
            val inputBytes = base64DecodeBytes(input) ?: return ""
            val keyBytes = key.toByteArray()
            val output = ByteArray(inputBytes.size)
            
            for (i in inputBytes.indices) {
                output[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            
            String(output)
        } catch (e: Exception) {
            LogUtils.e(TAG, "XOR解密失败", e)
            ""
        }
    }
    
    /**
     * 生成随机字符串
     */
    fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        
        return sb.toString()
    }
    
    /**
     * 生成随机数字字符串
     */
    fun generateRandomNumberString(length: Int): String {
        val chars = "0123456789"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        
        return sb.toString()
    }
    
    /**
     * 检查密码强度
     */
    fun checkPasswordStrength(password: String): PasswordStrength {
        var score = 0
        
        // 长度
        when {
            password.length >= 12 -> score += 3
            password.length >= 8 -> score += 2
            password.length >= 6 -> score += 1
        }
        
        // 包含小写字母
        if (password.any { it.isLowerCase() }) score += 1
        
        // 包含大写字母
        if (password.any { it.isUpperCase() }) score += 1
        
        // 包含数字
        if (password.any { it.isDigit() }) score += 1
        
        // 包含特殊字符
        if (password.any { !it.isLetterOrDigit() }) score += 2
        
        return when {
            score >= 7 -> PasswordStrength.STRONG
            score >= 5 -> PasswordStrength.MEDIUM
            score >= 3 -> PasswordStrength.WEAK
            else -> PasswordStrength.VERY_WEAK
        }
    }
    
    /**
     * 密码强度枚举
     */
    enum class PasswordStrength {
        VERY_WEAK,  // 非常弱
        WEAK,       // 弱
        MEDIUM,     // 中等
        STRONG      // 强
    }
    
    /**
     * 获取密码强度描述
     */
    fun getPasswordStrengthDescription(strength: PasswordStrength): String {
        return when (strength) {
            PasswordStrength.VERY_WEAK -> Loc.s(R.string.s0911)
            PasswordStrength.WEAK -> Loc.s(R.string.s0912)
            PasswordStrength.MEDIUM -> Loc.s(R.string.s0913)
            PasswordStrength.STRONG -> Loc.s(R.string.s0914)
        }
    }
    
    /**
     * 哈希密码（用于存储）
     * 注意：生产环境应该使用更安全的密码哈希算法（如bcrypt、Argon2）
     */
    fun hashPassword(password: String, salt: String): String {
        return sha256(password + salt)
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(password: String, salt: String, hash: String): Boolean {
        return hashPassword(password, salt) == hash
    }
}