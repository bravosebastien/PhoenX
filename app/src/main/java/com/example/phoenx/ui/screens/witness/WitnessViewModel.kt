package com.example.phoenx.ui.screens.witness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class WitnessEntity(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val status: String = "pending", // "pending" | "submitted"
    val submittedAt: Long? = null,
    val allowCreatorToRead: Boolean = false
)

@HiltViewModel
class WitnessViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _witnesses = MutableStateFlow<List<WitnessEntity>>(emptyList())
    val witnesses: StateFlow<List<WitnessEntity>> = _witnesses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inviteSuccess = MutableSharedFlow<Boolean>()
    val inviteSuccess: SharedFlow<Boolean> = _inviteSuccess.asSharedFlow()

    init {
        loadWitnesses()
    }

    fun loadWitnesses() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("witnesses")
                    .orderBy("name")
                    .get()
                    .await()
                
                val list = snapshot.documents.map { doc ->
                    WitnessEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        status = if (doc.getTimestamp("submittedAt") != null) "submitted" else "pending",
                        submittedAt = doc.getTimestamp("submittedAt")?.toDate()?.time,
                        allowCreatorToRead = doc.getBoolean("allowCreatorToRead") ?: false
                    )
                }
                _witnesses.value = list
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error loading witnesses", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inviteWitness(name: String, email: String, allowRead: Boolean, creatorName: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Créer le document témoin
                val witnessData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "allowCreatorToRead" to allowRead,
                    "submittedAt" to null,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                val docRef = db.collection("users").document(userId).collection("witnesses").add(witnessData).await()
                
                // 2. Appeler la Cloud Function pour générer le lien et envoyer l'email
                val data = hashMapOf(
                    "creatorId" to userId,
                    "witnessId" to docRef.id,
                    "witnessEmail" to email,
                    "witnessName" to name,
                    "creatorName" to creatorName
                )
                functions.getHttpsCallable("sendWitnessInvitation").call(data).await()
                
                _inviteSuccess.emit(true)
                loadWitnesses()
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error inviting witness", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteWitness(witnessId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("users").document(userId)
                    .collection("witnesses")
                    .document(witnessId)
                    .delete()
                    .await()
                loadWitnesses()
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error deleting witness", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- CÔTÉ TÉMOIN (Réponse) ---

    fun submitTestimony(
        creatorId: String,
        witnessId: String,
        token: String,
        testimonyText: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Chiffrer le témoignage (E2EE Tink)
                val encrypted = android.util.Base64.encodeToString(
                    encryptionManager.encryptText(testimonyText),
                    android.util.Base64.DEFAULT
                )

                // 2. Sauvegarder et verrouiller
                val witnessRef = db.collection("users").document(creatorId)
                    .collection("witnesses").document(witnessId)
                
                db.runBatch { batch ->
                    batch.update(witnessRef, mapOf(
                        "content" to encrypted,
                        "submittedAt" to com.google.firebase.Timestamp.now(),
                        "inviteToken" to com.google.firebase.firestore.FieldValue.delete()
                    ))
                }.await()
                
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("WitnessVM", "Error submitting testimony", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
