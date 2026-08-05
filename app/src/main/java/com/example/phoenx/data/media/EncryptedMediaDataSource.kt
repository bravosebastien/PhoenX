package com.example.phoenx.data.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.*
import com.example.phoenx.data.encryption.EncryptionManager
import java.io.InputStream
import javax.crypto.CipherInputStream

/**
 * EncryptedMediaDataSource (Signature PHOEN-X 5.0)
 * Permet à ExoPlayer de lire un fichier chiffré (.enc) depuis le local ou le Cloud.
 * v9.4.27 : Correction du bug de réinitialisation du flux (BadTagException).
 */
@UnstableApi
class EncryptedMediaDataSource(
    private val encryptionManager: EncryptionManager,
    private val explicitKey: ByteArray? = null
) : BaseDataSource(true) {

    private var upstream: DataSource? = null
    private var cipherInputStream: InputStream? = null
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        
        // Log de diagnostic pour la clé
        val hasKey = explicitKey != null || encryptionManager.getSessionKey() != null
        android.util.Log.d("MediaViewerDiag", "EncryptedMediaDataSource.open() - Uri: $uri, HasKey: $hasKey")

        val isRemote = uri.scheme?.startsWith("http") == true
        upstream = if (isRemote) DefaultHttpDataSource.Factory().createDataSource() else FileDataSource()

        val actualLength = upstream!!.open(dataSpec)
        
        // 1. Lire l'IV (12 octets)
        val iv = ByteArray(12)
        var totalIvRead = 0
        while (totalIvRead < 12) {
            val read = upstream!!.read(iv, totalIvRead, 12 - totalIvRead)
            if (read == C.RESULT_END_OF_INPUT) throw java.io.IOException("Fichier trop court pour l'IV")
            totalIvRead += read
        }

        // 2. Initialiser le Cipher
        val cipher = try {
            encryptionManager.getDecryptionCipher(iv, explicitKey)
        } catch (e: Exception) {
            android.util.Log.e("MediaViewerDiag", "ÉCHEC initialisation Cipher: ${e.message}")
            throw e
        }
        
        // 3. Wrapper simple pour éviter de ré-ouvrir l'upstream (v9.4.27)
        val simpleInputStream = object : InputStream() {
            override fun read(): Int {
                val b = ByteArray(1)
                return if (read(b, 0, 1) == -1) -1 else b[0].toInt() and 0xFF
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return upstream!!.read(b, off, len)
            }
        }
        
        cipherInputStream = CipherInputStream(simpleInputStream, cipher)
        
        opened = true
        transferStarted(dataSpec)
        
        return if (actualLength == C.LENGTH_UNSET.toLong()) C.LENGTH_UNSET.toLong() else actualLength - 12
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val read = cipherInputStream?.read(buffer, offset, length) ?: -1
        if (read == -1) return C.RESULT_END_OF_INPUT
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = upstream?.uri

    override fun close() {
        if (opened) {
            opened = false
            try {
                cipherInputStream?.close()
            } finally {
                cipherInputStream = null
                upstream?.close()
                upstream = null
                transferEnded()
            }
        }
    }
}

@UnstableApi
class EncryptedMediaDataSourceFactory(
    private val encryptionManager: EncryptionManager,
    private val explicitKey: ByteArray? = null
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return EncryptedMediaDataSource(encryptionManager, explicitKey)
    }
}
