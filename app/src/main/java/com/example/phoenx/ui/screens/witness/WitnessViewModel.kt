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
    val submittedAt: Long? = null
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

    fun loadWitnesses() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(userId).collection("witnesses").get().await()
                val list = snapshot.documents.map { doc ->
                    WitnessEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        status = if (doc.getTimestamp("submittedAt") != null) "submitted" else "pending",
                        submittedAt = doc.getTimestamp("submittedAt")?.toDate()?.time
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

    fun inviteWitness(name: String, email: String, creatorName: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Créer le document témoin
                val witnessData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "revealed" to false,
                    "submittedAt" to null
                )
                val docRef = db.collection("users").document(userId).collection("witnesses").add(witnessData).await()
                
                // 2. Appeler la Cloud Function pour le token et l'email
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

    fun submitTestimony(
        creatorId: String,
        witnessId: String,
        token: String,
        answers: Map<String, String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Vérifier le token
                val verifyData = hashMapOf("creatorId" to creatorId, "witnessId" to witnessId, "token" to token)
                functions.getHttpsCallable("verifyWitnessToken").call(verifyData).await()
                
                // 2. Préparer le témoignage (concaténation)
                val structuredText = answers.entries.joinToString("\n\n") { "${it.key}\n${it.value}" }

                // 3. Chiffrer
                val encryptedTestimony = android.util.Base64.encodeToString(
                    encryptionManager.encryptText(structuredText),
                    android.util.Base64.DEFAULT
                )

                // 4. Sauvegarder ATOMIQUEMENT
                val witnessRef = db.collection("users").document(creatorId).collection("witnesses").document(witnessId)
                
                db.runBatch { batch ->
                    batch.update(witnessRef, mapOf(
                        "encryptedTestimony" to encryptedTestimony,
                        "submittedAt" to com.google.firebase.Timestamp.now(),
                        "revealed" to false,
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
