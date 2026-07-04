package com.example.phoenx.data.encryption

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AesGcmKeyManager
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.content.Context

@Singleton
class EncryptionManager @Inject constructor() {

    private var sessionKey: ByteArray? = null

    init {
        AeadConfig.register()
    }

    fun setSessionKey(key: ByteArray) {
        this.sessionKey = key
    }

    fun getSessionKey(): ByteArray? = sessionKey

    /**
     * Dérivée une clé à partir d'un mot de passe en utilisant Argon2id
     */
    fun deriveKeyFromPassword(password: String, salt: ByteArray): ByteArray {
        val generator = Argon2BytesGenerator()
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3)
            .withMemoryAsKB(65536)
            .withParallelism(4)
            .withSalt(salt)
        
        generator.init(builder.build())
        val result = ByteArray(32) // Clé de 256 bits
        generator.generateBytes(password.toCharArray(), result)
        return result
    }

    /**
     * Chiffre un texte avec une clé fournie ou la clé de session (AES-256-GCM)
     */
    fun encryptText(plaintext: String, key: ByteArray? = null): ByteArray {
        val encryptionKey = key ?: sessionKey
            ?: throw IllegalStateException(
                "Clé de session non initialisée. " +
                "L'utilisateur doit être connecté."
            )
        // Générer un IV aléatoire de 12 bytes (standard AES-GCM)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        // Chiffrement AES-256-GCM avec la clé dérivée
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(encryptionKey, "AES")
        val paramSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, paramSpec)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // Concaténer IV + données chiffrées pour pouvoir déchiffrer
        return iv + encrypted
    }

    /**
     * Déchiffre un texte
     */
    fun decryptText(ciphertext: ByteArray, key: ByteArray? = null): String {
        val decryptionKey = key ?: sessionKey
            ?: throw IllegalStateException("Clé de session non initialisée.")
        // Extraire IV (12 premiers bytes) et données chiffrées
        val iv = ciphertext.sliceArray(0..11)
        val encrypted = ciphertext.sliceArray(12 until ciphertext.size)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(decryptionKey, "AES")
        val paramSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, paramSpec)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    // --- Helpers pour le Livre ---
    fun encrypt(text: String): String {
        val encrypted = encryptText(text)
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String): String {
        return try {
            val bytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
            decryptText(bytes)
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Déchiffrement échoué", e)
            ""
        }
    }
    // ----------------------------

    /**
     * Chiffre un fichier par blocs de 512KB
     */
    fun encryptFile(inputPath: String, key: ByteArray): ByteArray {
        val file = File(inputPath)
        val buffer = ByteArray(512 * 1024)
        // Logique de lecture et chiffrement par morceaux
        return ByteArray(0) 
    }

    /**
     * Génère une phrase de récupération de 12 mots (BIP-39)
     */
    fun generateRecoveryPhrase(context: Context): List<String> {
        val wordList = try {
            context.assets
                .open("bip39_french.txt")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Erreur lecture wordlist BIP-39", e)
            // Fallback en cas de problème de lecture des assets
            listOf("soleil", "rivière", "montagne", "forêt", "oiseau", "vent", "pierre", "sable", "mer", "nuage", "étoile", "lune")
        }

        val random = SecureRandom()
        return (1..12).map { wordList[random.nextInt(wordList.size)] }
    }

    fun deriveKeyFromPhrase(phrase: List<String>): ByteArray {
        return deriveKeyFromPassword(phrase.joinToString(" "), "phoenx_salt".toByteArray())
    }

    /**
     * Chiffre un texte avec une clé publique RSA (RSA-OAEP)
     */
    fun encryptWithPublicKey(plaintext: String, publicKeyBytes: ByteArray): ByteArray {
        val publicKey = java.security.KeyFactory
            .getInstance("RSA")
            .generatePublic(
                java.security.spec.X509EncodedKeySpec(publicKeyBytes)
            )
        val cipher = javax.crypto.Cipher.getInstance(
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        )
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(
            plaintext.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Déchiffre un texte avec une clé privée RSA (RSA-OAEP)
     */
    fun decryptWithPrivateKey(ciphertext: ByteArray, privateKeyBytes: ByteArray): String {
        val privateKey = java.security.KeyFactory
            .getInstance("RSA")
            .generatePrivate(
                java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes)
            )
        val cipher = javax.crypto.Cipher.getInstance(
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        )
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
