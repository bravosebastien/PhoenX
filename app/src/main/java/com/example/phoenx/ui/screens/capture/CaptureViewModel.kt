package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.LocalAnalysis
import com.example.phoenx.data.ai.OnDeviceAIManager
import com.example.phoenx.data.audio.PhoenXAudioRecorder
import com.example.phoenx.data.audio.SpeechToTextManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.haptic.HapticManager
import com.example.phoenx.data.local.*
import com.example.phoenx.domain.model.*
import com.example.phoenx.domain.util.EnigmaUtils
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import javax.inject.Inject

import android.net.Uri
import androidx.core.app.ActivityCompat
import android.content.Context
import android.util.Log
import android.widget.Toast
import android.location.Geocoder
import androidx.work.*
import com.example.phoenx.data.sync.SyncWorker
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileOutputStream
import java.io.InputStream

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val onDeviceAIManager: OnDeviceAIManager,
    private val audioRecorder: PhoenXAudioRecorder,
    private val hapticManager: HapticManager,
    private val sttManager: SpeechToTextManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _recipients = MutableStateFlow<List<RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<RecipientEntity>> = _recipients.asStateFlow()

    // Personnes citées unifiées (v9.4.26)
    private val _suggestedPersons = MutableStateFlow<List<SimplifiedPerson>>(emptyList())
    val suggestedPersons: StateFlow<List<SimplifiedPerson>> = _suggestedPersons.asStateFlow()

    private val _selectedPersons = MutableStateFlow<List<SimplifiedPerson>>(emptyList())
    val selectedPersons: StateFlow<List<SimplifiedPerson>> = _selectedPersons.asStateFlow()

    private val _selectedRecipientIds = MutableStateFlow<List<String>>(emptyList())
    val selectedRecipientIds: StateFlow<List<String>> = _selectedRecipientIds.asStateFlow()

    // Vocal
    val isSttListening = sttManager.isListening
    val sttPartialText = sttManager.partialText

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _preselectedLocationName = MutableStateFlow<String?>(null)
    val preselectedLocationName: StateFlow<String?> = _preselectedLocationName.asStateFlow()

    private val _suggestPin = MutableStateFlow(false)
    val suggestPin: StateFlow<Boolean> = _suggestPin.asStateFlow()

    private val _detectedLocation = MutableStateFlow<DetectedLocation?>(null)
    val detectedLocation: StateFlow<DetectedLocation?> = _detectedLocation.asStateFlow()

    data class DetectedLocation(
        val latitude: Double,
        val longitude: Double,
        val placeName: String
    )

    init {
        loadRecipients()
    }

    fun checkLocationForPin(context: Context) {
        viewModelScope.launch {
            // ... (logique existante)
        }
    }

    fun setPreselectedLocation(locationId: String?, name: String? = null) {
        if (locationId == null) return
        if (name != null) {
            _preselectedLocationName.value = name
        }
        
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId)
                    .collection("locations").document(locationId).get().await()
                val fetchedName = doc.getString("placeName")
                if (fetchedName != null) {
                    _preselectedLocationName.value = fetchedName
                }
            } catch (e: Exception) {
                Log.w("CaptureVM", "Offline: using preselected location name")
            }
        }
    }

    fun confirmPin(loc: DetectedLocation) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).collection("locations").add(mapOf(
                    "latitude" to loc.latitude,
                    "longitude" to loc.longitude,
                    "placeName" to loc.placeName,
                    "emoji" to "📍",
                    "memoriesCount" to 1,
                    "visitedAt" to System.currentTimeMillis()
                )).await()
                _suggestPin.value = false
            } catch (e: Exception) {}
        }
    }

    fun dismissPin() {
        _suggestPin.value = false
        _detectedLocation.value = null
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { list ->
                _recipients.value = list
            }
        }
    }

    fun startVocalCapture(currentText: String) {
        _transcript.value = currentText
        sttManager.startListening { finalResult ->
            if (finalResult.isNotEmpty()) {
                val buffer = _transcript.value
                _transcript.value = if (buffer.isEmpty()) {
                    finalResult
                } else {
                    "$buffer $finalResult"
                }
            }
        }
    }

    fun stopVocalCapture() {
        sttManager.stopListening()
    }

    fun appendTranscript(text: String) {
        _transcript.value = text
    }

    // --- GESTION DES PERSONNES UNIFIÉE (v9.4.26) ---

    private var searchJob: Job? = null

    fun searchPersons(query: String) {
        searchJob?.cancel() // Point 2 : Annulation de la recherche précédente
        if (query.isBlank()) {
            _suggestedPersons.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Point 2 : Debounce pour stabiliser la saisie
            
            try {
                // Point 1 : Combinaison des 4 tables via extensions (v9.4.26)
                val persons = offlineEntryDao.getAllPersons().first().toSimplified()
                val recipientsList = offlineEntryDao.getAllRecipients().first().toSimplifiedRecipient()
                val witnesses = offlineEntryDao.getAllWitnesses().first().toSimplifiedWitness()
                val depositaries = offlineEntryDao.getAllDepositaries().first().toSimplifiedDepositary()

                val allSimplified = persons + recipientsList + witnesses + depositaries

                // Déduplication & Filtrage
                val filtered = allSimplified
                    .filter { it.name.contains(query, ignoreCase = true) }
                    .distinctBy { it.name.lowercase().trim() }
                
                _suggestedPersons.value = filtered
            } catch (e: Exception) {
                Log.e("CaptureVM", "Erreur recherche personnes", e)
            }
        }
    }

    fun toggleRecipient(id: String) {
        _selectedRecipientIds.update { current ->
            if (current.contains(id)) current.filter { it != id }.distinct()
            else (current + id).distinct()
        }
    }

    fun selectPerson(person: SimplifiedPerson) {
        _selectedPersons.update { current ->
            if (current.any { it.id == person.id || it.name.lowercase() == person.name.lowercase() }) current 
            else current + person
        }
        _suggestedPersons.value = emptyList()
    }

    fun selectMe() {
        val user = auth.currentUser ?: return
        val me = SimplifiedPerson(
            id = "ME_${user.uid}",
            name = user.displayName ?: "Moi",
            photoUrl = user.photoUrl?.toString(),
            sourceType = "auteur",
            relationship = "Auteur",
            isMe = true
        )
        _selectedPersons.update { current ->
            if (current.any { it.isMe }) current else current + me
        }
    }

    fun removePerson(personId: String) {
        _selectedPersons.value = _selectedPersons.value.filter { it.id != personId }
    }

    fun createAndSelectPerson(
        firstName: String,
        lastName: String?,
        relationship: String?,
        distinctionType: String?,
        distinctionValue: String?,
        imageUri: Uri?,
        characterType: String = "HUMAN"
    ) {
        viewModelScope.launch {
            var finalImagePath: String? = null
            
            // Sauvegarde locale du portrait Cameo (v8.9.9)
            if (imageUri != null) {
                try {
                    val cameoDir = File(context.filesDir, "cameos")
                    if (!cameoDir.exists()) cameoDir.mkdirs()
                    
                    val fileName = "cameo_${UUID.randomUUID()}.jpg"
                    val destFile = File(cameoDir, fileName)
                    
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    finalImagePath = destFile.absolutePath
                    Log.d("CameoDebug", "Portrait sauvegardé : $finalImagePath")
                } catch (e: Exception) {
                    Log.e("CameoDebug", "Erreur sauvegarde portrait", e)
                }
            }

            try {
                val newPerson = com.example.phoenx.data.local.PersonEntity(
                    firstName = firstName,
                    lastName = lastName,
                    relationship = relationship,
                    distinctionType = distinctionType,
                    distinctionValue = distinctionValue,
                    imagePath = finalImagePath,
                    characterType = characterType
                )
                offlineEntryDao.insertPerson(newPerson)
                
                // Point 1 : Conversion pour le sélecteur unifié
                val simplified = SimplifiedPerson(
                    id = newPerson.id,
                    name = newPerson.firstName + (newPerson.lastName?.let { l -> " $l" } ?: ""),
                    photoUrl = newPerson.imagePath,
                    sourceType = "arbre_livre",
                    relationship = newPerson.relationship
                )
                selectPerson(simplified)

                // v8.9.9 : Déclenchement immédiat de la synchronisation pour la nouvelle personne
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)
                Log.d("PersonSync", "Synchronisation déclenchée pour la nouvelle personne : $firstName")
            } catch (_: Exception) { }
        }
    }

    private var currentAudioFile: File? = null

    fun startAudioRecording(cacheDir: File) {
        val file = File(cacheDir, "temp_capture_${System.currentTimeMillis()}.mp4")
        currentAudioFile = file
        audioRecorder.start(file)
        hapticManager.signalStartRecording()
        _uiState.value = CaptureUiState.RecordingAudio
    }

    fun stopAudioRecording() {
        audioRecorder.stop()
        _uiState.value = CaptureUiState.Idle
    }

    /**
     * Convertit un Uri Android (Galerie) en File temporaire utilisable par saveEntry.
     */
    fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_gallery_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("CaptureVM", "Erreur lors de la conversion Uri -> File : ${e.message}")
            null
        }
    }

    private val _newEntryId = MutableSharedFlow<String>()
    val newEntryId = _newEntryId.asSharedFlow()

    fun saveEntry(
        content: String?,
        mediaFile: File? = null,
        type: String = Screen.Capture.TYPE_TEXT,
        category: String = "Sagesse",
        visibility: String = "RESTRICTED",
        recipientIds: List<String> = emptyList(),
        silentAttribution: Boolean = false,
        isYoungSelfLetter: Boolean = false,
        targetAge: Int? = null,
        pendingQuestionId: String? = null,
        enigmaQuestion: String? = null,
        enigmaAnswer: String? = null,
        enigmaHint: String? = null,
        enigmaAutoUnlockDays: Int? = null,
        scheduledTimestamp: Long? = null,
        pactId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        locationId: String? = null,
        parentEntryId: String? = null,
        includeInBook: Boolean = true,
        soulTone: String? = null,
        onSuccess: (String) -> Unit = {} // v9.4.26
    ) {
        val user = auth.currentUser ?: return
        _uiState.value = CaptureUiState.Loading

        viewModelScope.launch {
            try {
                val rawText = content ?: "Nouveau souvenir"
                
                // 1. ANALYSE IA LOCALE SIMPLIFIÉE POUR LE SQUELETTE
                val analysis = LocalAnalysis(
                    summary = rawText, 
                    tags = emptyList(),
                    emotionalTone = "Neutral", 
                    lifePeriod = "Current"
                )
                
                // 2. CALCUL DE L'ÂGE
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                // 3. CHIFFREMENT
                val encrypted = encryptionManager.encryptText(rawText)
                
                // 4. SAUVEGARDE HORS-LIGNE IMMÉDIATE
                val entryId = UUID.randomUUID().toString()
                
                val entry = OfflineEntry(
                    id = entryId,
                    creatorUid = user.uid,
                    encryptedPayload = encrypted,
                    entryType = type,
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = category,
                    visibility = visibility,
                    recipientIds = recipientIds.joinToString(","),
                    createdAt = System.currentTimeMillis(),
                    aiSummary = analysis.summary,
                    locationName = locationName ?: _preselectedLocationName.value,
                    locationId = locationId,
                    includeInBook = includeInBook,
                    syncStatus = "pending"
                )
                
                offlineEntryDao.insertEntry(entry)
                
                // Déclenchement sync
                SyncWorker.trigger(context)

                _uiState.value = CaptureUiState.Success
                onSuccess(entryId)
            } catch (e: Exception) {
                Log.e("CaptureVM", "Error saving skeleton", e)
                _uiState.value = CaptureUiState.Error(e.message ?: "Erreur")
            }
        }
    }
}

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object RecordingAudio : CaptureUiState()
    object Loading : CaptureUiState()
    object Success : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}
