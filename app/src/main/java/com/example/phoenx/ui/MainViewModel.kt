package com.example.phoenx.ui

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.domain.liveness.LivenessManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import com.example.phoenx.service.SilenceManager
import com.example.phoenx.service.SilenceStatus
import com.example.phoenx.ui.theme.AccentPrimary
import com.google.firebase.firestore.FirebaseFirestore
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.PhoenXDatabase
import com.example.phoenx.domain.model.UserRole
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    private val functions: FirebaseFunctions,
    private val database: PhoenXDatabase
) : ViewModel() {

    private var userListener: ListenerRegistration? = null
    private var invitationListener: ListenerRegistration? = null

    private val _silenceStatus = MutableStateFlow<SilenceStatus?>(null)
    val silenceStatus: StateFlow<SilenceStatus?> = _silenceStatus.asStateFlow()

    private val _isCreator = MutableStateFlow<Boolean?>(null)
    val isCreator: StateFlow<Boolean?> = _isCreator.asStateFlow()

    private val _myRoles = MutableStateFlow<Map<String, UserRole>>(emptyMap())
    val myRoles: StateFlow<Map<String, UserRole>> = _myRoles.asStateFlow()

    private val _pendingInvitations = MutableStateFlow<List<PendingInvitation>>(emptyMap<String, Any>().values.toList().filterIsInstance<PendingInvitation>()) // Initialisation typée vide
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
    val firstProtectedCreatorId: StateFlow<String?> = _protectedCreatorIds
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _daysSinceLastCheckIn = MutableStateFlow(0)
    val daysSinceLastCheckIn: StateFlow<Int> = _daysSinceLastCheckIn.asStateFlow()
    
    val isSilenceOnboardingDone: StateFlow<Boolean?> = preferenceManager.isSilenceOnboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _silenceRhythmDays = MutableStateFlow(30)
    val silenceRhythmDays: StateFlow<Int> = _silenceRhythmDays.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    val accentColor: StateFlow<Int> = preferenceManager.accentColor
        .map { it ?: AccentPrimary.toArgb() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentPrimary.toArgb())

    val backgroundColor: StateFlow<Int> = preferenceManager.backgroundColor
        .map { it ?: 0xFF00FFFF.toInt() } // Néon par défaut pour le fond (selon XML)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF00FFFF.toInt())

    val backgroundStyle: StateFlow<String> = preferenceManager.backgroundStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "RADIAL")

    val isVoiceModeActive: StateFlow<Boolean> = preferenceManager.isVoiceModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    init {
        checkInactivity()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            checkSilenceOnLaunch(userId)
        }
    }

    fun setAccentColor(color: Int) {
        viewModelScope.launch {
            preferenceManager.setAccentColor(color)
        }
    }

    fun setBackgroundColor(color: Int) {
        viewModelScope.launch {
            preferenceManager.setBackgroundColor(color)
        }
    }

    fun setBackgroundStyle(style: String) {
        viewModelScope.launch {
            preferenceManager.setBackgroundStyle(style)
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            encryptionManager.setSessionKey(null)
            
            // Détachement des écouteurs temps réel
            userListener?.remove()
            userListener = null
            invitationListener?.remove()
            invitationListener = null

            _isCreator.value = null
            _myRoles.value = emptyMap()
            _pendingInvitations.value = emptyList()
            _protectedCreatorIds.value = emptyList()
            _isDepositaryAccount.value = null
            _silenceStatus.value = null
            
            // Nettoyage de la base de données Room pour éviter les données résiduelles
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        invitationListener?.remove()
    }

    /**
     * Écoute le profil utilisateur en temps réel pour gérer le routage et les clés.
     */
    fun checkSilenceOnLaunch(userId: String) {
        // --- RÉPARATION V20 (Backfill creatorUid) ---
        viewModelScope.launch(Dispatchers.IO) {
            val count = database.offlineEntryDao().repairEmptyCreatorUids(userId)
            if (count > 0) android.util.Log.d("PHOENX_V20", "$count souvenirs réparés pour l'UID $userId")
        }

        // Nettoyage de l'ancien écouteur si existant
        userListener?.remove()

        userListener = db.collection("users").document(userId).addSnapshotListener { doc, error ->
            if (error != null || doc == null || !doc.exists()) {
                android.util.Log.e("MainViewModel", "Erreur écoute profil", error)
                return@addSnapshotListener
            }

            val name = doc.getString("displayName") ?: doc.getString("email")?.substringBefore("@") ?: "Ami"
            _userName.value = name
            _userEmail.value = doc.getString("email") ?: ""

            // --- 1. GESTION DES RÔLES ET MIGRATION (Restauration v7.2) ---
            val rolesData = doc.get("myRoles") as? Map<String, Any>
            val legacyIds = doc.get("protectedCreatorIds") as? List<String> ?: emptyList()

            // Si besoin, on déclenche la migration Firestore en arrière-plan
            if (rolesData == null && legacyIds.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        functions.getHttpsCallable("migrateLegacyRoles").call().await()
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "Échec migration auto", e)
                    }
                }
            }

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
                        sourceId = map["sourceId"] as? String,
                        joinedAt = (map["joinedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                    )
                }
            } else if (legacyIds.isNotEmpty()) {
                // Fallback mémoire pendant la migration (v7.2 Resilience)
                legacyIds.forEach { cId ->
                    parsedRoles["${cId}_depositary"] = UserRole(creatorId = cId, role = "depositary", status = "active", label = "Gardien")
                }
            }
            _myRoles.value = parsedRoles

            // --- 2. DÉTERMINATION DU STATUT (v7.6 Résilient) ---
            val isCreatorVal = if (doc.contains("isCreator")) {
                doc.getBoolean("isCreator") == true
            } else if (doc.contains("isDepositaryOnly")) {
                doc.getBoolean("isDepositaryOnly") != true
            } else {
                parsedRoles.isEmpty() // true si rien, false si invité
            }
            
            _isCreator.value = isCreatorVal
            _isDepositaryAccount.value = !isCreatorVal

            if (isCreatorVal) {
                viewModelScope.launch {
                    val status = silenceManager.checkSilenceStatus(userId)
                    _silenceStatus.value = status

                    // Sync AES
                    if (encryptionManager.getSessionKey() == null) {
                        doc.getString("encryptionKey")?.let {
                            encryptionManager.setSessionKey(android.util.Base64.decode(it, android.util.Base64.NO_WRAP))
                        }
                    }

                    // Sync RSA
                    val localPublicKey = encryptionManager.ensureRsaKeyPairExists()
                    if (doc.getString("publicEncryptionKey") != localPublicKey) {
                        db.collection("users").document(userId).update("publicEncryptionKey", localPublicKey)
                    }
                }
            } else {
                parsedRoles.values.firstOrNull()?.let {
                    _protectedCreatorIds.value = listOf(it.creatorId)
                }
            }

            // --- 3. CONFIG ET STATS SILENCE (Restauration v7.5) ---
            _silenceRhythmDays.value = doc.getLong("silenceConfig.rhythmDays")?.toInt() ?: 30
            val lastCheckIn = doc.getTimestamp("silenceConfig.lastCheckInAt")
            if (lastCheckIn != null) {
                val diff = System.currentTimeMillis() - lastCheckIn.toDate().time
                _daysSinceLastCheckIn.value = (diff / (1000 * 60 * 60 * 24)).toInt()
            }

            // --- 4. RADAR D'INVITATIONS (v7.6) ---
            val userMail = doc.getString("email")
            if (userMail != null) {
                // Nettoyage de l'ancien radar si existant (ex: changement d'email ou re-lancement)
                invitationListener?.remove()

                invitationListener = db.collection("invitations")
                    .whereEqualTo("email", userMail.lowercase())
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
        // Sauvegarder localement IMMEDIATEMENT pour stopper l'onboarding
        preferenceManager.setSilenceOnboardingDone(true)
        _silenceRhythmDays.value = rhythmDays
        
        try {
            db.collection("users").document(userId).update(
                mapOf(
                    "isCreator" to true, // S'assure que le rôle est validé
                    "silenceConfig.rhythmDays" to rhythmDays,
                    "silenceConfig.lastCheckInAt" to com.google.firebase.Timestamp.now(),
                    "silenceConfig.missedCycles" to 0,
                    "silenceConfig.escalationLevel" to 0,
                    "silenceConfig.lastSilenceStatus" to "present"
                )
            ).await()
        } catch (e: Exception) {
            // Si Firestore échoue, on garde quand même le flag local pour ne pas harceler l'utilisateur
        }
    }

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shouldShowWelcomeGuide: StateFlow<Boolean?> = preferenceManager.shouldShowWelcomeGuide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isVideoBannerDismissed: StateFlow<Boolean> = preferenceManager.isVideoBannerDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private fun checkInactivity() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (protocolManager.checkInactivity(userId)) {
                // Protocole en attente si nécessaire
            }
        }
    }

    /**
     * Appelée à chaque fois que l'utilisateur est considéré comme "actif" sur l'app.
     */
    fun confirmPresence() {
        livenessManager.confirmPassivePresence()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Déclenche le check RSA/Silence même si l'app était déjà en RAM
            checkSilenceOnLaunch(userId)
        }
    }

    fun toggleVoiceMode() {
        viewModelScope.launch {
            val current = isVoiceModeActive.value
            preferenceManager.setVoiceModeActive(!current)
            if (!current) {
                voiceManager.speak("Mode vocal activé. Je vous écoute.")
            } else {
                voiceManager.speak("Mode vocal désactivé.")
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setBiometricEnabled(enabled)
        }
    }

    fun dismissWelcomeGuide(neverShowAgain: Boolean) {
        viewModelScope.launch {
            if (neverShowAgain) {
                preferenceManager.setShouldShowWelcomeGuide(false)
            }
        }
    }

    fun dismissVideoBanner() {
        viewModelScope.launch {
            preferenceManager.setVideoBannerDismissed(true)
        }
    }

    fun resetVideoBanner() {
        viewModelScope.launch {
            preferenceManager.setVideoBannerDismissed(false)
        }
    }

    fun isDepositaryOnboardingSeen(userId: String): Flow<Boolean> = 
        preferenceManager.isDepositaryOnboardingSeen(userId)

    fun markDepositaryOnboardingSeen(userId: String) {
        viewModelScope.launch {
            preferenceManager.setDepositaryOnboardingSeen(userId, true)
        }
    }

    fun handleVoiceCommand(command: String, navigate: (String) -> Unit) {
        val cmd = command.lowercase()
        when {
            cmd.contains("pensée") || cmd.contains("souvenir") || cmd.contains("écrire") -> navigate("capture/TEXT")
            cmd.contains("voix") || cmd.contains("enregistrer") -> navigate("capture/AUDIO")
            cmd.contains("photo") || cmd.contains("image") -> navigate("capture/PHOTO")
            cmd.contains("fil") || cmd.contains("timeline") || cmd.contains("historique") -> navigate("fil")
            cmd.contains("nuit") || cmd.contains("sommeil") -> navigate("capture/NIGHT")
            cmd.contains("accueil") || cmd.contains("maison") -> navigate("home")
            cmd.contains("ia") || cmd.contains("essence") || cmd.contains("portrait") -> navigate("essence")
            cmd.contains("meilleurs") || cmd.contains("favoris") -> navigate("favorites")
            cmd.contains("questions") || cmd.contains("histoire") -> navigate("questions")
            cmd.contains("aide") -> voiceManager.speak("Vous pouvez dire : 'Ouvre mon fil', 'Dépose une pensée', 'Enregistre ma voix', ou 'Retour à l'accueil'.")
            cmd.contains("retour") -> navigate("back")
        }
    }
    
    fun speak(text: String) {
        if (isVoiceModeActive.value) {
            voiceManager.speak(text)
        }
    }
}
