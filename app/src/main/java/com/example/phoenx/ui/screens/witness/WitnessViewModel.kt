package com.example.phoenx.ui.screens.witness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.WitnessEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class WitnessViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: EncryptionManager,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _witnesses = MutableStateFlow<List<WitnessEntity>>(emptyList())
    val witnesses: StateFlow<List<WitnessEntity>> = _witnesses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _inviteSuccess = MutableSharedFlow<Boolean>()
    val inviteSuccess: SharedFlow<Boolean> = _inviteSuccess.asSharedFlow()

    private val _creatorName = MutableStateFlow<String?>(null)
    val creatorName: StateFlow<String?> = _creatorName.asStateFlow()

    private val _witnessConfig = MutableStateFlow<WitnessConfig?>(null)
    val witnessConfig: StateFlow<WitnessConfig?> = _witnessConfig.asStateFlow()

    data class WitnessConfig(
        val allowRead: Boolean,
        val allowReject: Boolean,
        val publicKey: String?,
        val submittedAt: Long? = null
    )

    init {
        loadWitnesses()
    }

    fun verifyToken(creatorId: String, witnessId: String, token: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mutableMapOf(
                    "creatorId" to creatorId,
                    "witnessId" to witnessId
                )
                if (token != null && token != "none") {
                    data["token"] = token
                }
                
                val result = functions.getHttpsCallable("verifyWitnessToken").call(data).await()
                val resData = result.data as Map<*, *>
                
                _creatorName.value = resData["creatorName"] as? String
                _witnessConfig.value = WitnessConfig(
                    allowRead = resData["allowCreatorToRead"] as? Boolean ?: false,
                    allowReject = resData["allowCreatorToReject"] as? Boolean ?: false,
                    publicKey = resData["publicEncryptionKey"] as? String,
                    submittedAt = (resData["submittedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                )
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error verifying token/UID", e)
                _error.value = "Lien invalide ou accès refusé."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWitnesses() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Écouter Room (Réactivité)
        viewModelScope.launch {
            offlineEntryDao.getAllWitnesses().collectLatest { list ->
                _witnesses.value = list
            }
        }

        // 2. Synchroniser depuis Firestore
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("witnesses")
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    val witness = WitnessEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        status = doc.getString("status") ?: (if (doc.getTimestamp("submittedAt") != null) "submitted" else "invited"),
                        submittedAt = doc.getTimestamp("submittedAt")?.toDate()?.time,
                        allowCreatorToRead = doc.getBoolean("allowCreatorToRead") ?: false,
                        allowCreatorToReject = doc.getBoolean("allowCreatorToReject") ?: false
                    )
                    offlineEntryDao.insertWitness(witness)
                }
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error syncing witnesses", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inviteWitness(name: String, email: String, allowRead: Boolean, allowReject: Boolean, creatorName: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Créer le document témoin sur Firestore
                val witnessData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "allowCreatorToRead" to allowRead,
                    "allowCreatorToReject" to allowReject,
                    "status" to "invited",
                    "submittedAt" to null,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                val docRef = db.collection("users").document(userId).collection("witnesses").add(witnessData).await()
                
                // 2. Sauvegarde locale immédiate
                offlineEntryDao.insertWitness(WitnessEntity(
                    id = docRef.id,
                    name = name,
                    email = email,
                    allowCreatorToRead = allowRead,
                    allowCreatorToReject = allowReject,
                    status = "invited"
                ))

                // 3. Appeler la Cloud Function Universelle (v7.2)
                val inviteData = hashMapOf(
                    "email" to email,
                    "role" to "witness",
                    "sourceId" to docRef.id,
                    "label" to "Témoin"
                )
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteData).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String

                // 4. Envoi de l'email (on garde la structure mais avec le nouveau lien)
                val emailData = hashMapOf(
                    "to" to email,
                    "message" to hashMapOf(
                        "subject" to "$creatorName demande ton témoignage",
                        "text" to "Lien pour rejoindre son cercle : https://phoenx.app/join/$tokenId"
                    )
                )
                db.collection("mail").add(emailData).await()
                
                _inviteSuccess.emit(true)
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error inviting witness", e)
                _error.value = "Impossible d'envoyer l'invitation. Vérifie ta connexion."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deleteWitness(witnessId: String) {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("WitnessVM", "Suppression demandée pour id=$witnessId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Supprimer sur Firestore
                db.collection("users").document(userId)
                    .collection("witnesses")
                    .document(witnessId)
                    .delete()
                    .await()
                
                android.util.Log.d("WitnessVM", "Suppression Firestore réussie pour id=$witnessId")
                
                // Supprimer localement
                offlineEntryDao.deleteWitness(witnessId)
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error deleting witness", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Récupère et déchiffre le contenu d'un témoignage (si autorisé).
     */
    suspend fun getTestimonyContent(witnessId: String): String? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val doc = db.collection("users").document(userId)
                .collection("witnesses").document(witnessId).get().await()
            
            val encryptedBase64 = doc.getString("content") ?: return null
            val encryptedBytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
            
            // On tente le déchiffrement RSA (car droit de regard/lecture implique RSA)
            encryptionManager.decryptWithPrivateKey(encryptedBytes)
        } catch (e: Exception) {
            android.util.Log.e("WitnessVM", "Erreur lecture témoignage", e)
            null
        }
    }

    fun reviewTestimony(witnessId: String, accept: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val newStatus = if (accept) "validated" else "rejected"
        
        viewModelScope.launch {
            try {
                // Mettre à jour Firestore
                db.collection("users").document(userId)
                    .collection("witnesses").document(witnessId)
                    .update("status", newStatus)
                    .await()
                
                // Mettre à jour Room
                val current = _witnesses.value.find { it.id == witnessId }
                current?.let {
                    offlineEntryDao.insertWitness(it.copy(status = newStatus))
                }
            } catch (e: Exception) {
                _error.value = "Erreur lors de la mise à jour du statut."
            }
        }
    }

    // --- CÔTÉ TÉMOIN (Réponse) ---

    fun submitTestimony(
        creatorId: String,
        witnessId: String,
        token: String?,
        testimonyText: String,
        onSuccess: () -> Unit
    ) {
        val config = witnessConfig.value
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Chiffrement conditionnel (RSA si droit de regard ou de lecture)
                val encryptedBytes = if (config?.allowRead == true || config?.allowReject == true) {
                    val pubKeyBase64 = config.publicKey 
                        ?: throw Exception("Clé publique du Créateur manquante")
                    val pubKeyBytes = android.util.Base64.decode(pubKeyBase64, android.util.Base64.NO_WRAP)
                    encryptionManager.encryptWithPublicKey(testimonyText, pubKeyBytes)
                } else {
                    // Chiffrement AES standard (héritiers)
                    encryptionManager.encryptText(testimonyText)
                }

                val encryptedBase64 = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT)

                // 2. Appel de la Cloud Function pour sauvegarder (Sécurité v7.2 Hybride)
                val data = mutableMapOf(
                    "creatorId" to creatorId,
                    "witnessId" to witnessId,
                    "encryptedContent" to encryptedBase64
                )
                if (token != null && token != "none") {
                    data["token"] = token
                }
                
                functions.getHttpsCallable("submitWitnessTestimony").call(data).await()
                
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error submitting testimony", e)
                _error.value = "Échec de l'envoi du témoignage. Réessaie plus tard."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
