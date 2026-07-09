package com.example.phoenx.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import com.google.crypto.tink.aead.AeadConfig
import java.security.*
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.util.*

@Singleton
class EncryptionManager @Inject constructor() {

    private var sessionKey: ByteArray? = null
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    private val RSA_ALIAS = "phoenx_rsa_key"

    init {
        AeadConfig.register()
    }

    fun setSessionKey(key: ByteArray) {
        this.sessionKey = key
    }

    fun getSessionKey(): ByteArray? = sessionKey

    /**
     * Dérive une clé à partir d'un mot de passe en utilisant Argon2id
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
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(encryptionKey, "AES")
        val paramSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return iv + encrypted
    }

    /**
     * Déchiffre un texte
     */
    fun decryptText(ciphertext: ByteArray, key: ByteArray? = null): String {
        val decryptionKey = key ?: sessionKey
            ?: throw IllegalStateException("Clé de session non initialisée.")
        val iv = ciphertext.sliceArray(0..11)
        val encrypted = ciphertext.sliceArray(12 until ciphertext.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(decryptionKey, "AES")
        val paramSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun encrypt(text: String): String {
        val encrypted = encryptText(text)
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String): String {
        return try {
            val bytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            decryptText(bytes)
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Déchiffrement échoué", e)
            ""
        }
    }

    fun generateRecoveryPhrase(context: Context): List<String> {
        val wordList = try {
            context.assets
                .open("bip39_french.txt")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Erreur lecture wordlist BIP-39", e)
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
        val publicKey = KeyFactory
            .getInstance("RSA")
            .generatePublic(
                java.security.spec.X509EncodedKeySpec(publicKeyBytes)
            )
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    /**
     * Vérifie si la paire de clés RSA existe localement dans le Keystore.
     */
    fun hasLocalRsaKey(): Boolean = keyStore.containsAlias(RSA_ALIAS)

    /**
     * Garantit l'existence d'une paire de clés RSA.
     * Si elle n'existe pas, elle est générée dans le Keystore matériel.
     * Retourne la clé publique encodée en Base64.
     */
    fun ensureRsaKeyPairExists(): String {
        if (!hasLocalRsaKey()) {
            val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                "AndroidKeyStore"
            )
            val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
                RSA_ALIAS,
                KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
            ).run {
                setKeySize(2048)
                setDigests(KeyProperties.DIGEST_SHA256)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                setUserAuthenticationRequired(false)
                build()
            }
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }

        val publicKey = keyStore.getCertificate(RSA_ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    private fun getPrivateKeyForDecryption(): PrivateKey {
        return keyStore.getKey(RSA_ALIAS, null) as PrivateKey
    }

    /**
     * Déchiffre un texte avec la clé privée RSA du Keystore (RSA-OAEP).
     * Auto-réparation : si la clé est absente (ex: changement d'appareil),
     * elle est générée (mais les anciens messages resteront indéchiffrables).
     */
    fun decryptWithPrivateKey(ciphertext: ByteArray): String {
        if (!hasLocalRsaKey()) {
            ensureRsaKeyPairExists()
        }
        val privateKey = getPrivateKeyForDecryption()
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
