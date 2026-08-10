package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.data.local.StandaloneMediaEntity
import com.example.phoenx.data.local.StandaloneMediaDao
import com.example.phoenx.data.media.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class RecipientContentDashboard(
    val souvenirs: List<OfflineEntry> = emptyList(),
    val photos: List<com.example.phoenx.domain.model.PhoenXEntry> = emptyList(),
    val videos: List<com.example.phoenx.domain.model.PhoenXEntry> = emptyList(),
    val audios: List<com.example.phoenx.domain.model.PhoenXEntry> = emptyList()
)

@HiltViewModel
class RecipientViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val mediaManager: MediaManager
) : ViewModel() {

    private val _transmissionAmbiance = MutableStateFlow(AmbianceState())
    val transmissionAmbiance: StateFlow<AmbianceState> = _transmissionAmbiance.asStateFlow()

    private val _uiState = MutableStateFlow<RecipientUiState>(RecipientUiState.Loading)
    val uiState: StateFlow<RecipientUiState> = _uiState

    /**
     * Retourne le nombre de questions en attente pour un destinataire précis (v9.4.27)
     */
    fun getPendingQuestionsCount(recipientId: String): Flow<Int> {
        val userId = auth.currentUser?.uid ?: return flowOf(0)
        return callbackFlow {
            val listener = db.collection("users").document(userId)
                .collection("pendingQuestions")
                .whereEqualTo("recipientId", recipientId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    trySend(snapshot?.size() ?: 0)
                }
            awaitClose { listener.remove() }
        }
    }

    init {
        loadRecipients()
    }

    fun getEntriesForRecipient(recipientId: String): Flow<List<OfflineEntry>> = 
        offlineEntryDao.getEntriesForRecipient(recipientId)

    fun getEntriesForRecipientUnified(recipientId: String): Flow<List<OfflineEntry>> = 
        offlineEntryDao.getEntriesForRecipientUnified(recipientId)

    /**
     * Retourne les souvenirs assignés à un destinataire par son UID (v9.4.27)
     */
    fun getEntriesForRecipientUid(recipientUid: String?): Flow<List<OfflineEntry>> {
        if (recipientUid == null) return flowOf(emptyList())
        return offlineEntryDao.getAllEntries().map { entries ->
            entries.filter { 
                it.parentEntryId == null && (it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(recipientUid))
            }
        }
    }

    fun getPortraitForRecipient(recipientId: String): Flow<OfflineEntry?> = 
        offlineEntryDao.getPortraitEntryForRecipient(recipientId)

    /**
     * Dashboard du contenu attribué (v9.4.27)
     * Utilise l'UID (linkedUid) pour le filtrage sécurisé.
     */
    fun getAssignedContent(recipientUid: String?): Flow<RecipientContentDashboard> {
        if (recipientUid == null) return flowOf(RecipientContentDashboard())

        val entriesFlow = offlineEntryDao.getAllEntries()
        val standaloneFlow = standaloneMediaDao.getAllStandaloneMedia()

        return combine(entriesFlow, standaloneFlow) { entries, standalone ->
            // Filtrage unifié : EVERYONE ou présence de l'UID dans recipientIds (CSV)
            val filteredEntries = entries.filter { 
                it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(recipientUid) 
            }
            val filteredStandalone = standalone.filter { 
                it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(recipientUid) 
            }

            // v9.4.27 : Unification du mapping en PhoenXEntry pour le Dashboard
            val allMapped = filteredEntries.map { it.toSimpleDomain() } + filteredStandalone.map { it.toSimpleStandaloneDomain() }

            RecipientContentDashboard(
                souvenirs = filteredEntries.filter { it.parentEntryId == null && it.entryType != "PORTRAIT" },
                photos = allMapped.filter { it.type == com.example.phoenx.domain.model.EntryType.PHOTO },
                videos = allMapped.filter { it.type == com.example.phoenx.domain.model.EntryType.VIDEO },
                audios = allMapped.filter { it.type == com.example.phoenx.domain.model.EntryType.AUDIO }
            )
        }
    }

    // Mappers simplifiés pour le Dashboard (v9.4.27)
    private fun OfflineEntry.toSimpleDomain() = com.example.phoenx.domain.model.PhoenXEntry(
        id = id,
        aiSummary = aiSummary,
        type = when(entryType) {
            "PHOTO" -> com.example.phoenx.domain.model.EntryType.PHOTO
            "VIDEO" -> com.example.phoenx.domain.model.EntryType.VIDEO
            "AUDIO", "EMOTION" -> com.example.phoenx.domain.model.EntryType.AUDIO
            else -> com.example.phoenx.domain.model.EntryType.THOUGHT
        },
        parentEntryId = parentEntryId,
        mediaUrl = mediaUrl,
        localMediaPath = localMediaPath,
        mediaProvider = mediaProvider,
        userComment = userComment,
        ageAtCreation = com.example.phoenx.domain.model.AgeSnapshot(0, 0, 0),
        encryptedContent = ByteArray(0)
    )

    private fun StandaloneMediaEntity.toSimpleStandaloneDomain() = com.example.phoenx.domain.model.PhoenXEntry(
        id = id,
        aiSummary = title,
        type = when(type) {
            "PHOTO" -> com.example.phoenx.domain.model.EntryType.PHOTO
            "YOUTUBE", "VIDEO" -> com.example.phoenx.domain.model.EntryType.VIDEO
            "SPOTIFY", "DEEZER" -> com.example.phoenx.domain.model.EntryType.AUDIO
            else -> com.example.phoenx.domain.model.EntryType.THOUGHT
        },
        mediaUrl = if (type != "TEXT_EXCERPT") content else null,
        mediaProvider = mediaProvider ?: type,
        userComment = userComment,
        ageAtCreation = com.example.phoenx.domain.model.AgeSnapshot(0, 0, 0),
        encryptedContent = ByteArray(0)
    )

    private fun loadRecipients() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Écouter les changements locaux (Room)
        viewModelScope.launch {
            try {
                offlineEntryDao.getAllRecipients().collectLatest { recipients ->
                    _uiState.value = RecipientUiState.Success(recipients)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur chargement Room", e)
                _uiState.value = RecipientUiState.Success(emptyList())
            }
        }

        // 2. Synchroniser depuis Firestore
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("recipients").get().await()
                
                snapshot.documents.forEach { doc ->
                    val recipient = RecipientEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        canAskQuestions = doc.getBoolean("canAskQuestions") ?: false,
                        maxQuestionsAllowed = doc.getLong("maxQuestionsAllowed")?.toInt(),
                        linkedUid = doc.getString("linkedUid"), // v9.2
                        photoUrl = doc.getString("photoUrl") // v9.2.2
                    )
                    offlineEntryDao.insertRecipient(recipient)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur sync Firestore -> Room", e)
            }
        }
    }

    fun addRecipient(name: String, email: String, relationship: String, phone: String? = null, imageUri: android.net.Uri? = null) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                var finalPhotoUrl: String? = null
                
                // 1. Upload photo if present
                if (imageUri != null) {
                    try {
                        val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                            .child("users/$userId/recipients/${java.util.UUID.randomUUID()}.jpg")
                        ref.putFile(imageUri).await()
                        finalPhotoUrl = ref.path // Stockage du CHEMIN (v9.4.17)
                    } catch (e: Exception) {
                        android.util.Log.e("RecipientVM", "Erreur upload photo destinataire", e)
                    }
                }

                // 2. Sauvegarde Firestore
                val recipientData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "relationship" to relationship,
                    "status" to "invited",
                    "photoUrl" to finalPhotoUrl
                )
                val docRef = db.collection("users").document(userId)
                    .collection("recipients").add(recipientData).await()

                // 3. Room local
                val recipient = RecipientEntity(
                    id = docRef.id,
                    name = name,
                    email = email,
                    phone = phone,
                    relationship = relationship,
                    photoUrl = finalPhotoUrl
                )
                offlineEntryDao.insertRecipient(recipient)

                // 3. Invitation Universelle (v7.2)
                val inviteData = hashMapOf(
                    "email" to email,
                    "role" to "recipient",
                    "sourceId" to docRef.id,
                    "label" to "Destinataire"
                )
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteData).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String

                // 4. Envoi Email via Cloud Function (v9.4.10)
                val userDoc = db.collection("users").document(userId).get().await()
                val creatorName = userDoc.getString("displayName") ?: "Ton proche"
                
                val emailData = hashMapOf(
                    "to" to email,
                    "subject" to "$creatorName souhaite vous partager son histoire",
                    "text" to "Bonjour $name,\n\n$creatorName prépare son espace de souvenirs sur PHOEN-X et souhaite vous accorder sa confiance en vous choisissant comme destinataire de son récit de vie.\n\nLien pour rejoindre son cercle de confiance : https://phoenx.app/join/$tokenId"
                )
                functions.getHttpsCallable("sendMail").call(emailData).await()

            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur ajout destinataire", e)
            }
        }
    }

    fun deleteRecipient(recipient: RecipientEntity) {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("RecipientVM", "Suppression demandée pour id=${recipient.id}")
        viewModelScope.launch {
            try {
                // 1. Suppression Firestore
                db.collection("users").document(userId)
                    .collection("recipients").document(recipient.id)
                    .delete().await()

                android.util.Log.d("RecipientVM", "Suppression Firestore réussie pour id=${recipient.id}")

                // 2. Suppression Room local
                offlineEntryDao.deleteRecipient(recipient)
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur suppression destinataire", e)
            }
        }
    }

    fun updatePermissions(recipientId: String, canAsk: Boolean, maxQuestions: Int?) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val recipients = (uiState.value as? RecipientUiState.Success)?.recipients ?: return@launch
            val recipient = recipients.find { it.id == recipientId } ?: return@launch
            
            val updated = recipient.copy(
                canAskQuestions = canAsk,
                maxQuestionsAllowed = maxQuestions
            )
            offlineEntryDao.insertRecipient(updated)

            try {
                // Sync with Firestore
                db.collection("users").document(currentUserId)
                    .collection("recipients").document(recipientId)
                    .update(mapOf(
                        "canAskQuestions" to canAsk,
                        "maxQuestionsAllowed" to maxQuestions
                    )).await()

                // Trigger email if activated for the first time
                if (canAsk && !recipient.canAskQuestions) {
                    val userDoc = db.collection("users").document(currentUserId).get().await()
                    val creatorName = userDoc.getString("displayName") ?: "Ton proche"
                    
                    val inviteLink = "https://phoenx.app/ask?creator=$currentUserId&recipient=$recipientId"
                    
                    val data = hashMapOf(
                        "recipientEmail" to recipient.email,
                        "recipientName" to recipient.name,
                        "creatorName" to creatorName,
                        "inviteLink" to inviteLink
                    )
                    functions.getHttpsCallable("notifyQuestionRightGranted").call(data).await()
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Error updating permissions", e)
            }
        }
    }

    /**
     * Charge l'ambiance de transmission pour un proche précis (v9.4.27)
     */
    fun loadTransmissionAmbiance(recipientId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // v9.4.27 : On cherche dans le document du destinataire
                val doc = db.collection("users").document(userId)
                    .collection("recipients").document(recipientId).get().await()
                
                if (doc.exists()) {
                    _transmissionAmbiance.value = AmbianceState(
                        backgroundId = doc.getString("transmissionBackgroundId") ?: "classic_ivory",
                        fontId = doc.getString("transmissionFontId") ?: "playfair_display"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur ambiance", e)
            }
        }
    }

    /**
     * Sauvegarde l'ambiance pour un proche précis (v9.4.27)
     */
    fun saveTransmissionAmbiance(recipientId: String, backgroundId: String, fontId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "transmissionBackgroundId" to backgroundId,
                    "transmissionFontId" to fontId
                )
                db.collection("users").document(userId)
                    .collection("recipients").document(recipientId)
                    .update(data)
                    .await()
                
                // MAJ Locale Room
                offlineEntryDao.updateRecipientAmbiance(recipientId, backgroundId, fontId)
                
                _transmissionAmbiance.value = AmbianceState(backgroundId, fontId)
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur save ambiance", e)
            }
        }
    }
}

data class AmbianceState(
    val backgroundId: String = "classic_ivory",
    val fontId: String = "playfair_display"
)

sealed class RecipientUiState {
    object Loading : RecipientUiState()
    data class Success(val recipients: List<RecipientEntity>) : RecipientUiState()
}
