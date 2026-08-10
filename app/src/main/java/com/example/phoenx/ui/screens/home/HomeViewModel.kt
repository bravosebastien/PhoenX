package com.example.phoenx.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.model.PresentationVideo
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val aiManager: AIManager,
    private val protocolManager: ActivationProtocolManager,
    private val offlineEntryDao: OfflineEntryDao,
    private val preferenceManager: PreferenceManager // v9.4.26
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val hasSeenStepByStepNudge: StateFlow<Boolean> = preferenceManager.hasSeenStepByStepNudge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun markStepByStepNudgeSeen() {
        viewModelScope.launch {
            preferenceManager.setStepByStepNudgeSeen()
        }
    }

    private val _daysSincePresence = MutableStateFlow(0)
    val daysSincePresence: StateFlow<Int> = _daysSincePresence.asStateFlow()

    init {
        loadUserData()
        loadBiographerQuestion()
        observeLatestEntries()
        loadPendingQuestionsCount()
        loadExtraStats()
        fetchRemoteConfig()
        loadPresentationVideos()
        loadHomeCardsConfig() // v9.4.22
    }

    private fun loadHomeCardsConfig() {
        viewModelScope.launch {
            try {
                db.collection("appConfig").document("homeCards")
                    .addSnapshotListener { snapshot, _ ->
                        val url = snapshot?.getString("genealogyCardImageUrl")
                        _uiState.update { it.copy(genealogyCardImageUrl = url) }
                    }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Erreur chargement config homeCards", e)
            }
        }
    }

    private fun loadPresentationVideos() {
        viewModelScope.launch {
            try {
                db.collection("presentationVideos")
                    .orderBy("slotIndex") // v9.2.7 : Tri par slotIndex
                    .addSnapshotListener { snapshot, _ ->
                        val videos = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(PresentationVideo::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        _uiState.update { it.copy(presentationVideos = videos) }
                    }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Erreur chargement vidéos présentation: ${e.message}")
            }
        }
    }

    private fun fetchRemoteConfig() {
        android.util.Log.d("RemoteConfigDebug", "fetchRemoteConfig started")
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 heure en prod
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            android.util.Log.d("RemoteConfigDebug", "fetchAndActivate complete. Success: ${task.isSuccessful}")
            if (task.isSuccessful) {
                val url = remoteConfig.getString("default_book_cover_url").trim()
                val earthUrl = remoteConfig.getString("earth_texture_url").trim()
                android.util.Log.d("RemoteConfigDebug", "earth_texture_url from RC: '$earthUrl'")
                
                _uiState.update { it.copy(
                    defaultCoverUrl = url.ifEmpty { null },
                    earthTextureUrl = earthUrl.ifEmpty { null }
                ) }
            }
        }
    }

    /**
     * Calcule le nombre de jours écoulés depuis la dernière présence confirmée.
     */
    fun calculateDaysSincePresence(lastCheckInTimestamp: Long) {
        val diff = System.currentTimeMillis() - lastCheckInTimestamp
        _daysSincePresence.value = (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun loadExtraStats() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // Nombre de membres PHOEN-X (Global) v9.4.27
                db.collection("appConfig").document("stats")
                    .addSnapshotListener { snapshot, _ ->
                        val count = snapshot?.getLong("totalUsers")?.toInt() ?: 0
                        _uiState.update { it.copy(globalUserCount = count) }
                    }

                // Nombre de proches (Local Room) v9.4.27
                offlineEntryDao.getAllRecipients().collect { list ->
                    _uiState.update { it.copy(localRecipientCount = list.size) }
                }

                // Nombre de questions répondues
                db.collection("users").document(user.uid)
                    .collection("entries")
                    .whereNotEqualTo("enigmaQuestion", null)
                    .addSnapshotListener { snapshot, _ ->
                        _uiState.update { it.copy(answeredQuestionsCount = snapshot?.size() ?: 0) }
                    }

                // Nombre de chapitres validés (v9.2: Corrigé pour utiliser current_draft)
                db.collection("users").document(user.uid)
                    .collection("book").document("current_draft")
                    .addSnapshotListener { snapshot, _ ->
                        val chaptersList = snapshot?.get("chapters")
                        @Suppress("UNCHECKED_CAST")
                        val chapters = chaptersList as? List<Map<String, Any>> ?: emptyList()
                        val validatedCount = chapters.count { it["status"] == "VALIDATED" }
                        val title = snapshot?.getString("bookTitle")
                        val coverUrl = snapshot?.getString("coverImageUrl")
                        val style = snapshot?.getString("coverTitleStyle") ?: "GOLD"
                        val scale = snapshot?.getDouble("coverScale")?.toFloat() ?: 1f
                        val offsetX = snapshot?.getDouble("coverOffsetX")?.toFloat() ?: 0f
                        val offsetY = snapshot?.getDouble("coverOffsetY")?.toFloat() ?: 0f
                        
                        _uiState.update { it.copy(
                            validatedChaptersCount = validatedCount,
                            bookTitle = title,
                            coverImageUrl = coverUrl,
                            coverTitleStyle = style,
                            coverScale = scale,
                            coverOffsetX = offsetX,
                            coverOffsetY = offsetY
                        ) }
                    }
            } catch (e: Exception) {}
        }
    }

    private fun loadPendingQuestionsCount() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid)
                    .collection("pendingQuestions")
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshot, _ ->
                        _uiState.update { it.copy(pendingQuestionsCount = snapshot?.size() ?: 0) }
                    }
            } catch (e: Exception) {}
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("displayName") ?: user.email?.substringBefore("@") ?: "Ami"
                        val birthTimestamp = doc.getTimestamp("dateOfBirth")
                        val lastAlive = doc.getTimestamp("lastAliveConfirmedAt")
                        
                        var currentAge = 0
                        if (birthTimestamp != null) {
                            val birthDate = birthTimestamp.toDate()
                            val ageSnapshot = AgeUtils.calculateAge(birthDate)
                            currentAge = ageSnapshot.years
                        }

                        if (lastAlive != null) {
                            calculateDaysSincePresence(lastAlive.toDate().time)
                        }

                        _uiState.value = _uiState.value.copy(
                            userName = name,
                            userEmail = user.email ?: "",
                            photoUrl = doc.getString("photoUrl"),
                            currentAge = currentAge,
                            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        )
                    }
            } catch (e: Exception) { }
        }
    }

    private fun observeLatestEntries() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
                // Filtrer pour ne compter que les souvenirs racines (v8.3.4)
                val rootEntries = entries.filter { it.parentEntryId == null }
                
                val minAgeVal = if (rootEntries.isEmpty()) 0 else rootEntries.minOf { AgeUtils.parseAgeJson(it.ageAtCreation).years }
                
                _uiState.value = _uiState.value.copy(
                    latestEntries = rootEntries.take(5),
                    entryCount = rootEntries.size,
                    minAge = minAgeVal
                )
            }
        }
    }

    private fun loadBiographerQuestion() {
        viewModelScope.launch {
            try {
                val question = aiManager.getBiographerQuestion()
                _uiState.value = _uiState.value.copy(biographerQuestion = question)
            } catch (e: Exception) { }
        }
    }

    fun updateProofOfLife() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                protocolManager.confirmProofOfLife(userId)
                _daysSincePresence.value = 0
            } catch (e: Exception) { }
        }
    }

    /**
     * Déclenche le rattrapage des UIDs (v9.3.2) - Réservé Admin
     */
    fun runRecipientBackfill(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val result = com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("backfillRecipientUids")
                    .call()
                    .await()
                val data = result.data as Map<*, *>
                val count = (data["recipientsProcessed"] as? Number)?.toInt() ?: 0
                onComplete(count)
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Erreur backfill: ${e.message}")
            }
        }
    }

    /**
     * Déclenche le rattrapage des Dépositaires (v9.4.4) - Réservé Admin
     */
    fun runDepositaryBackfill(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val result = com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("backfillDepositaryUids")
                    .call()
                    .await()
                val data = result.data as Map<*, *>
                val count = (data["usersProcessed"] as? Number)?.toInt() ?: 0
                onComplete(count)
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Erreur backfill depositary: ${e.message}")
            }
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val photoUrl: String? = null,
    val currentDate: String = "",
    val entryCount: Int = 0,
    val minAge: Int = 0,
    val currentAge: Int = 0,
    val biographerQuestion: String = "Quelle décision as-tu prise dont tu es le plus fier ?",
    val pendingQuestionsCount: Int = 0,
    val answeredQuestionsCount: Int = 0,
    val validatedChaptersCount: Int = 0,
    val bookTitle: String? = null,
    val globalUserCount: Int = 0, // v9.4.27
    val localRecipientCount: Int = 0, // v9.4.27
    val coverImageUrl: String? = null, // v9.2.4
    val defaultCoverUrl: String? = null, // v9.2.5
    val earthTextureUrl: String? = null, // v9.2.8
    val coverTitleStyle: String = "GOLD", // v9.2.6
    val coverScale: Float = 1f,
    val coverOffsetX: Float = 0f,
    val coverOffsetY: Float = 0f,
    val genealogyCardImageUrl: String? = null, // v9.4.22
    val presentationVideos: List<PresentationVideo> = emptyList(), // v9.2.6
    val latestEntries: List<OfflineEntry> = emptyList()
)
