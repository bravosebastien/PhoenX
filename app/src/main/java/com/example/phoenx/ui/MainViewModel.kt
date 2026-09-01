package com.example.phoenx.ui

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.domain.liveness.LivenessManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import com.example.phoenx.service.SilenceManager
import com.example.phoenx.service.SilenceStatus
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.BackgroundPrimary
import com.google.firebase.firestore.FirebaseFirestore
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.PhoenXDatabase
import com.example.phoenx.domain.model.UserRole
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.phoenx.data.sync.InitialSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val protocolManager: ActivationProtocolManager,
    private val voiceManager: VoiceAccessibilityManager,
    private val livenessManager: LivenessManager,
    private val preferenceManager: PreferenceManager,
    private val silenceManager: SilenceManager,
    private val encryptionManager: EncryptionManager,
    private val mediaManager: MediaManager, // v9.7.4
    private val functions: FirebaseFunctions,
    val database: PhoenXDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val repairMutex = Mutex()
    private var isRepairCompleted = false

    private var userListener: ListenerRegistration? = null
    private var invitationListener: ListenerRegistration? = null
    private var personalitiesListener: ListenerRegistration? = null // v9.7.4

    private val _silenceStatus = MutableStateFlow<SilenceStatus?>(null)
    val silenceStatus: StateFlow<SilenceStatus?> = _silenceStatus.asStateFlow()

    private val _isCreator = MutableStateFlow<Boolean?>(null)
    val isCreator: StateFlow<Boolean?> = _isCreator.asStateFlow()

    private val _hasSeenBecomeCreatorPrompt = MutableStateFlow<Boolean>(false)
    val hasSeenBecomeCreatorPrompt: StateFlow<Boolean> = _hasSeenBecomeCreatorPrompt.asStateFlow()

    private val _currentPerspective = MutableStateFlow(Perspective.MY_MEMORY)
    val currentPerspective: StateFlow<Perspective> = _currentPerspective.asStateFlow()

    enum class Perspective { MY_MEMORY, HERITAGE, MISSIONS }

    fun switchPerspective(perspective: Perspective) {
        android.util.Log.d("PerspectiveDebug", "MainViewModel: switchPerspective requested to $perspective")
        _currentPerspective.value = perspective
    }

    private val _myRoles = MutableStateFlow<Map<String, UserRole>>(emptyMap())
    val myRoles: StateFlow<Map<String, UserRole>> = _myRoles.asStateFlow()

    private val _pendingInvitations = MutableStateFlow<List<PendingInvitation>>(emptyList())
    val pendingInvitations: StateFlow<List<PendingInvitation>> = _pendingInvitations.asStateFlow()

    data class PendingInvitation(
        val id: String,
        val creatorName: String,
        val role: String,
        val label: String
    )

    private val _isDepositaryAccount = MutableStateFlow<Boolean?>(null)
    val isDepositaryAccount: StateFlow<Boolean?> = _isDepositaryAccount.asStateFlow()

    private val _protectedCreatorIds = MutableStateFlow<List<String>>(emptyList())
    val firstProtectedCreatorId: StateFlow<String?> = _protectedCreatorIds.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _daysSinceLastCheckIn = MutableStateFlow(0)
    val daysSinceLastCheckIn: StateFlow<Int> = _daysSinceLastCheckIn.asStateFlow()

    val isSilenceOnboardingDone: StateFlow<Boolean?> = preferenceManager.isSilenceOnboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _silenceRhythmDays = MutableStateFlow(30)
    val silenceRhythmDays: StateFlow<Int> = _silenceRhythmDays.asStateFlow()

    private val _userName = MutableStateFlow("Ami")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl: StateFlow<String?> = _photoUrl.asStateFlow()

    private val _transmissionBackgroundId = MutableStateFlow("classic_ivory")
    val transmissionBackgroundId: StateFlow<String> = _transmissionBackgroundId.asStateFlow()

    private val _transmissionFontId = MutableStateFlow("playfair_display")
    val transmissionFontId: StateFlow<String> = _transmissionFontId.asStateFlow()

    val accentColor: StateFlow<Int> = preferenceManager.accentColor
        .map { it ?: AccentPrimary.toArgb() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentPrimary.toArgb())

    val backgroundColor: StateFlow<Int> = preferenceManager.backgroundColor
        .map { it ?: BackgroundPrimary.toArgb() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundPrimary.toArgb())

    val backgroundStyle: StateFlow<String> = preferenceManager.backgroundStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SOLID")

    val isVoiceModeActive: StateFlow<Boolean> = preferenceManager.isVoiceModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        val user = auth.currentUser
        if (user != null) {
            checkSilenceOnLaunch(user.uid)
        }
    }

    fun setAccentColor(color: Int) {
        viewModelScope.launch { preferenceManager.setAccentColor(color) }
    }

    fun setBackgroundColor(color: Int) {
        viewModelScope.launch { preferenceManager.setBackgroundColor(color) }
    }

    fun setBackgroundStyle(style: String) {
        viewModelScope.launch { preferenceManager.setBackgroundStyle(style) }
    }

    fun logout() {
        viewModelScope.launch {
            userListener?.remove()
            invitationListener?.remove()
            personalitiesListener?.remove()

            auth.signOut()
            _isCreator.value = null
            _myRoles.value = emptyMap()
            _protectedCreatorIds.value = emptyList()
            _currentPerspective.value = Perspective.MY_MEMORY
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        invitationListener?.remove()
        personalitiesListener?.remove()
    }

    fun checkSilenceOnLaunch(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isRepairCompleted) return@launch
            repairMutex.withLock {
                if (isRepairCompleted) return@launch
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    database.offlineEntryDao().repairEmptyCreatorUids(userId)
                    
                    val aesKey = userDoc.getString("encryptionKey")
                    if (aesKey != null) ensureLegacyKey(userId, aesKey)

                    val syncRequest = OneTimeWorkRequestBuilder<InitialSyncWorker>().build()
                    WorkManager.getInstance(context).enqueue(syncRequest)
                    
                    isRepairCompleted = true
                    
                    // v9.7.4 : Démarrage des écouteurs temps-réel pour le Créateur
                    startPersonalitiesListener(userId)
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error in repair/sync launch", e)
                }
            }
        }

        userListener?.remove()
        userListener = db.collection("users").document(userId).addSnapshotListener { doc, error ->
            if (error != null || doc == null || !doc.exists()) return@addSnapshotListener

            val name = doc.getString("displayName") ?: doc.getString("email")?.substringBefore("@") ?: "Ami"
            _userName.value = name
            _userEmail.value = doc.getString("email") ?: ""
            _photoUrl.value = doc.getString("photoUrl")

            val bgId = doc.getString("transmissionBackgroundId") ?: "classic_ivory"
            val fontId = doc.getString("transmissionFontId") ?: "playfair_display"
            
            viewModelScope.launch {
                preferenceManager.setGlobalTheme(bgId, fontId)
            }

            _transmissionBackgroundId.value = bgId
            _transmissionFontId.value = fontId

            val rolesData = doc.get("myRoles") as? Map<String, Any>
            val parsedRoles = mutableMapOf<String, UserRole>()
            if (rolesData != null) {
                rolesData.forEach { (key, value) ->
                    val map = value as? Map<String, Any> ?: return@forEach
                    parsedRoles[key] = UserRole(
                        creatorId = map["creatorId"] as? String ?: "",
                        creatorName = map["creatorName"] as? String ?: "Votre proche",
                        role = map["role"] as? String ?: "",
                        status = map["status"] as? String ?: "",
                        label = map["label"] as? String ?: "",
                        photoUrl = map["creatorPhotoUrl"] as? String,
                        sourceId = map["sourceId"] as? String,
                        joinedAt = (map["joinedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                    )
                }
            }
            _myRoles.value = parsedRoles

            val isCreatorVal = doc.getBoolean("isCreator") ?: (parsedRoles.isEmpty())
            _isCreator.value = isCreatorVal
            _isDepositaryAccount.value = !isCreatorVal

            if (isCreatorVal) {
                viewModelScope.launch {
                    _silenceStatus.value = silenceManager.checkSilenceStatus(userId)
                    if (encryptionManager.getSessionKey() == null) {
                        doc.getString("encryptionKey")?.let {
                            encryptionManager.setSessionKey(android.util.Base64.decode(it, android.util.Base64.NO_WRAP))
                        }
                    }
                }
            }
        }

        invitationListener?.remove()
        auth.currentUser?.email?.let { email ->
            invitationListener = db.collection("invitations")
                .whereEqualTo("email", email.lowercase())
                .whereEqualTo("used", false)
                .addSnapshotListener { inviteSnap, _ ->
                    val invites = inviteSnap?.documents?.map { iDoc ->
                        PendingInvitation(
                            id = iDoc.id,
                            creatorName = iDoc.getString("creatorName") ?: "Un proche",
                            role = iDoc.getString("role") ?: "",
                            label = iDoc.getString("label") ?: "Nouvelle mission"
                        )
                    } ?: emptyList()
                    _pendingInvitations.value = invites
                }
        }
    }

    private fun startPersonalitiesListener(userId: String) {
        android.util.Log.d("PHOENX_SYNC_PERSO", "MainViewModel: Enregistrement du listener pour $userId")
        personalitiesListener?.remove()
        personalitiesListener = db.collection("users").document(userId)
            .collection("personalities")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("PHOENX_SYNC_PERSO", "MainViewModel: Erreur listener", error)
                    return@addSnapshotListener
                }
                android.util.Log.d("PHOENX_SYNC_PERSO", "MainViewModel: Événement Firestore reçu (${snapshot?.documentChanges?.size ?: 0} changements)")

                snapshot?.documentChanges?.forEach { change ->
                    val doc = change.document
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val mainPhotoUrl = doc.getString("mainPhotoPath")
                                    var finalLocalPath: String? = null

                                    if (!mainPhotoUrl.isNullOrBlank() && !mainPhotoUrl.startsWith("/")) {
                                        try {
                                            val persoDir = File(context.filesDir, "personalities")
                                            if (!persoDir.exists()) persoDir.mkdirs()
                                            val destFile = File(persoDir, "main_${doc.id}.jpg")
                                            mediaManager.downloadCameo(mainPhotoUrl, destFile)
                                            finalLocalPath = destFile.absolutePath
                                            android.util.Log.d("PHOENX_SYNC_PERSO", "Photo téléchargée en temps réel pour: ${doc.id}")
                                        } catch (e: Exception) {
                                            android.util.Log.e("PHOENX_SYNC_PERSO", "Erreur download temps réel photo", e)
                                        }
                                    }

                                    val entity = com.example.phoenx.data.local.PersonalityEntity(
                                        id = doc.id,
                                        name = doc.getString("name") ?: "",
                                        category = doc.getString("category") ?: "Autre",
                                        customCategoryLabel = doc.getString("customCategoryLabel"),
                                        mainPhotoPath = finalLocalPath ?: mainPhotoUrl ?: "",
                                        biography = doc.getString("biography") ?: "",
                                        personalComment = doc.getString("personalComment") ?: "",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        syncStatus = "synced"
                                    )
                                    database.personalityDao().insertPersonality(entity)
                                    android.util.Log.d("PHOENX_SYNC_PERSO", "MainViewModel: Sync Temps-réel (ADD/MOD): ${entity.name}")
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    database.personalityDao().getPersonalityByIdSync(doc.id)?.let {
                                        database.personalityDao().deletePersonality(it)
                                        android.util.Log.d("PHOENX_SYNC_PERSO", "MainViewModel: Sync Temps-réel (REMOVE): ${it.name}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PHOENX_SYNC_PERSO", "MainViewModel: ÉCHEC traitement changement", e)
                        }
                    }
                }
            }
        android.util.Log.d("PHOENX_SYNC_PERSO", "MainViewModel: Listener personnalités enregistré avec succès")
    }

    private fun ensureLegacyKey(userId: String, currentKeyBase64: String?) {
        if (currentKeyBase64 == null) return
        viewModelScope.launch {
            try {
                val legacyKeyRef = db.collection("users").document(userId).collection("entry_keys").document("main")
                if (!legacyKeyRef.get().await().exists()) {
                    legacyKeyRef.set(mapOf("key" to currentKeyBase64)).await()
                }
            } catch (_: Exception) {}
        }
    }

    fun recordCheckIn(status: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            silenceManager.recordCheckIn(userId, status)
            _silenceStatus.value = SilenceStatus.OK
        }
    }

    suspend fun setSilenceConfig(rhythmDays: Int) {
        val userId = auth.currentUser?.uid ?: return
        preferenceManager.setSilenceOnboardingDone(true)
        _silenceRhythmDays.value = rhythmDays
        try {
            functions.getHttpsCallable("becomeCreator").call(hashMapOf("rhythmDays" to rhythmDays)).await()
            _isCreator.value = true
        } catch (_: Exception) {}
    }

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shouldShowWelcomeGuide: StateFlow<Boolean?> = preferenceManager.shouldShowWelcomeGuide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isVideoBannerDismissed: StateFlow<Boolean> = preferenceManager.isVideoBannerDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun confirmPresence() {
        livenessManager.confirmPassivePresence()
        auth.currentUser?.uid?.let { checkSilenceOnLaunch(it) }
    }

    fun toggleVoiceMode() {
        viewModelScope.launch {
            val current = isVoiceModeActive.value
            preferenceManager.setVoiceModeActive(!current)
            voiceManager.speak(if (!current) "Mode vocal activé." else "Mode vocal désactivé.")
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setBiometricEnabled(enabled) }
    }

    fun markBecomeCreatorPromptSeen() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update("hasSeenBecomeCreatorPrompt", true).await()
                _hasSeenBecomeCreatorPrompt.value = true
            } catch (_: Exception) {}
        }
    }

    fun dismissWelcomeGuide(seenEverywhere: Boolean) {
        viewModelScope.launch { preferenceManager.setShouldShowWelcomeGuide(false) }
    }

    fun dismissVideoBanner() {
        viewModelScope.launch { preferenceManager.setVideoBannerDismissed(true) }
    }

    fun resetVideoBanner() {
        viewModelScope.launch { preferenceManager.setVideoBannerDismissed(false) }
    }

    fun isDepositaryOnboardingSeen(userId: String): Flow<Boolean> = preferenceManager.isDepositaryOnboardingSeen(userId)

    fun markDepositaryOnboardingSeen(userId: String) {
        viewModelScope.launch { preferenceManager.setDepositaryOnboardingSeen(userId, true) }
    }

    fun checkProtocolStatus(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("getCreatorProtocolStatus").call(hashMapOf("creatorId" to userId)).await()
                val response = result.data as Map<*, *>
                onResult(response["isActivated"] as? Boolean ?: false)
            } catch (_: Exception) { onResult(false) }
        }
    }

    fun handleVoiceCommand(cmd: String, speak: (String) -> Unit) {
        when {
            cmd.contains("fil") -> speak("J'ouvre votre fil.")
            cmd.contains("dépose") -> speak("Atelier d'écriture.")
            cmd.contains("aide") -> voiceManager.speak("Dites 'Ouvre mon fil' ou 'Dépose une pensée'.")
        }
    }

    fun speak(text: String) {
        if (isVoiceModeActive.value) voiceManager.speak(text)
    }
}
