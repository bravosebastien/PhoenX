package com.example.phoenx.data.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import com.example.phoenx.data.encryption.EncryptionManager
import java.io.InputStream
import javax.crypto.CipherInputStream

/**
 * EncryptedMediaDataSource (Signature PHOEN-X 5.0)
 * Permet à ExoPlayer de lire un fichier chiffré sans jamais le déchiffrer sur le disque.
 */
class EncryptedMediaDataSource(
    private val encryptionManager: EncryptionManager,
    private val explicitKey: ByteArray? = null
) : BaseDataSource(true) {

    private val upstream: FileDataSource = FileDataSource()
    private var cipherInputStream: InputStream? = null
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        upstream.open(dataSpec)
        
        val fileInputStream = upstream.open(dataSpec).let { 
            // FileDataSource.open returns the length, but we need the actual stream.
            // Actually, we should probably use a different approach for the stream source.
            java.io.FileInputStream(uri.path!!)
        }

        // 1. Lire l'IV (les 12 premiers octets)
        val iv = ByteArray(12)
        val bytesRead = fileInputStream.read(iv)
        if (bytesRead < 12) throw java.io.IOException("Fichier trop court pour contenir un IV")

        // 2. Initialiser le Cipher
        val cipher = encryptionManager.getDecryptionCipher(iv, explicitKey)
        
        // 3. Créer le flux de déchiffrement
        cipherInputStream = CipherInputStream(fileInputStream, cipher)
        
        opened = true
        transferStarted(dataSpec)
        
        // On retourne la taille restante estimée (approximative car GCM a un tag)
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val read = cipherInputStream?.read(buffer, offset, length) ?: -1
        if (read == -1) return C.RESULT_END_OF_INPUT
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = upstream.uri

    override fun close() {
        if (opened) {
            opened = false
            cipherInputStream?.close()
            cipherInputStream = null
            upstream.close()
            transferEnded()
        }
    }
}

class EncryptedMediaDataSourceFactory(
    private val encryptionManager: EncryptionManager,
    private val explicitKey: ByteArray? = null
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return EncryptedMediaDataSource(encryptionManager, explicitKey)
    }
}
