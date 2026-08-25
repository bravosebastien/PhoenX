package com.example.phoenx.ui.screens.home

import android.util.Log
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
    private val preferenceManager: PreferenceManager
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
        // v9.4.29 : Rendre le chargement réactif à l'authentification
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadUserData()
                loadBiographerQuestion()
                observeLatestEntries()
                loadPendingQuestionsCount()
                loadExtraStats()
                loadPresentationVideos()
                loadHomeCardsConfig()
            }
        }
        fetchRemoteConfig()
    }

    private fun loadHomeCardsConfig() {
        val user = auth.currentUser ?: return
        db.collection("appConfig").document("homeCards")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PHOENX_HOME", "Erreur homeCards config: ${error.message}")
                    return@addSnapshotListener
                }
                val url = snapshot?.getString("genealogyCardImageUrl")
                _uiState.update { it.copy(genealogyCardImageUrl = url) }
            }
    }

    private fun loadPresentationVideos() {
        val user = auth.currentUser ?: return
        db.collection("presentationVideos")
            .orderBy("slotIndex")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PHOENX_HOME", "Erreur vidéos présentation: ${error.message}")
                    return@addSnapshotListener
                }
                val videos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PresentationVideo::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _uiState.update { it.copy(presentationVideos = videos) }
            }
    }

    private fun fetchRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val url = remoteConfig.getString("default_book_cover_url").trim()
                val earthUrl = remoteConfig.getString("earth_texture_url").trim()
                _uiState.update { it.copy(
                    defaultCoverUrl = url.ifEmpty { null },
                    earthTextureUrl = earthUrl.ifEmpty { null }
                ) }
            }
        }
    }

    fun calculateDaysSincePresence(lastCheckInTimestamp: Long) {
        val diff = System.currentTimeMillis() - lastCheckInTimestamp
        _daysSincePresence.value = (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun loadExtraStats() {
        val user = auth.currentUser
        Log.e("PHOENX_COVER_R", "loadExtraStats appelée, user=${user?.uid}")
        if (user == null) return

        // 1. ÉCOUTEUR COUVERTURE (Indépendant et Prioritaire)
        Log.e("PHOENX_COVER_R", "Installation du listener couverture...")
        db.collection("users").document(user.uid)
            .collection("book").document("current_draft")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PHOENX_COVER_R", "ERREUR Firestore Snapshot Couverture: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    Log.w("PHOENX_COVER_R", "Snapshot reçu mais document INEXISTANT")
                    return@addSnapshotListener
                }

                val title = snapshot.getString("bookTitle")
                val coverUrl = snapshot.getString("coverImageUrl")
                val style = snapshot.getString("coverTitleStyle") ?: "GOLD"
                val scale = snapshot.getDouble("coverScale")?.toFloat() ?: 1f
                val offsetX = snapshot.getDouble("coverOffsetX")?.toFloat() ?: 0f
                val offsetY = snapshot.getDouble("coverOffsetY")?.toFloat() ?: 0f
                
                Log.e("PHOENX_COVER_R", "Lecture Firestore RÉUSSIE: title=$title, style=$style, url=$coverUrl")

                val chaptersList = snapshot.get("chapters")
                @Suppress("UNCHECKED_CAST")
                val chapters = chaptersList as? List<Map<String, Any>> ?: emptyList()
                val validatedCount = chapters.count { it["status"] == "VALIDATED" }

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

        // 2. ÉCOUTEUR STATS COMMUNAUTÉ (Indépendant)
        db.collection("appConfig").document("stats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PHOENX_HOME", "Erreur stats globales: ${error.message}")
                    return@addSnapshotListener
                }
                val count = snapshot?.getLong("totalUsers")?.toInt() ?: 0
                _uiState.update { it.copy(globalUserCount = count) }
            }

        // 3. ÉCOUTEUR QUESTIONS RÉPONDUES (Indépendant)
        db.collection("users").document(user.uid)
            .collection("entries")
            .whereNotEqualTo("enigmaQuestion", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PHOENX_HOME", "Erreur questions: ${error.message}")
                    return@addSnapshotListener
                }
                _uiState.update { it.copy(answeredQuestionsCount = snapshot?.size() ?: 0) }
            }

        // 4. ÉCOUTEUR CERCLE DE CONFIANCE (Indépendant et BLOQUANT)
        // On le lance dans sa propre coroutine isolée car .collect est infini.
        viewModelScope.launch {
            Log.d("PHOENX_HOME", "Lancement du collect cercle...")
            offlineEntryDao.getAllRecipients().collect { list ->
                _uiState.update { it.copy(localRecipientCount = list.size) }
            }
        }
    }

    private fun loadPendingQuestionsCount() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("pendingQuestions")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PHOENX_HOME", "Erreur pending questions: ${error.message}")
                    return@addSnapshotListener
                }
                _uiState.update { it.copy(pendingQuestionsCount = snapshot?.size() ?: 0) }
            }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
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
    }

    private fun observeLatestEntries() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
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
    val globalUserCount: Int = 0,
    val localRecipientCount: Int = 0,
    val coverImageUrl: String? = null,
    val defaultCoverUrl: String? = null,
    val earthTextureUrl: String? = null,
    val coverTitleStyle: String = "GOLD",
    val coverScale: Float = 1f,
    val coverOffsetX: Float = 0f,
    val coverOffsetY: Float = 0f,
    val genealogyCardImageUrl: String? = null,
    val presentationVideos: List<PresentationVideo> = emptyList(),
    val latestEntries: List<OfflineEntry> = emptyList()
)
