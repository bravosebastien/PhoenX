package com.example.phoenx.data.encryption

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(context: Context) {

    private val keysetName = "phoenx_master_keyset"
    private val prefFileName = "phoenx_encryption_prefs"
    private val masterKeyUri = "android-keystore://phoenx_master_key"

    private val aead: Aead

    init {
        // 1. Initialiser la configuration Tink
        AeadConfig.register()

        // 2. Charger ou générer le keyset (jeu de clés) stocké de manière sécurisée
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, keysetName, prefFileName)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(masterKeyUri)
            .build()
            .keysetHandle

        // 3. Obtenir la primitive AEAD (Authenticated Encryption with Associated Data)
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    /**
     * Chiffre une chaîne de caractères (pour Firebase)
     */
    fun encrypt(plainText: String, associatedData: String = ""): ByteArray {
        return aead.encrypt(plainText.toByteArray(Charsets.UTF_8), associatedData.toByteArray(Charsets.UTF_8))
    }

    /**
     * Déchiffre des données provenant de Firebase
     */
    fun decrypt(cipherText: ByteArray, associatedData: String = ""): String {
        val decryptedByte = aead.decrypt(cipherText, associatedData.toByteArray(Charsets.UTF_8))
        return String(decryptedByte, Charsets.UTF_8)
    }
}
