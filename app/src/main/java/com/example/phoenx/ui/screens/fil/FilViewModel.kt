package com.example.phoenx.ui.screens.fil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.AmendmentEntity
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXAmendment
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.*
import javax.inject.Inject
import org.json.JSONObject

@HiltViewModel
class FilViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FilUiState>(FilUiState(isLoading = true))
    val uiState: StateFlow<FilUiState> = _uiState

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { offlineEntries ->
                val decodedEntries = offlineEntries.map { entry ->
                    val amendments = offlineEntryDao.getAmendmentsForEntrySync(entry.id).map { it.toDomain() }
                    entry.toDomain(encryptionManager, amendments)
                }
                
                _uiState.value = FilUiState(
                    entries = decodedEntries,
                    totalCount = decodedEntries.size,
                    minAge = decodedEntries.minOfOrNull { it.ageAtCreation.years } ?: 0,
                    maxAge = decodedEntries.maxOfOrNull { it.ageAtCreation.years } ?: 0,
                    isLoading = false
                )
            }
        }
    }

    fun addAmendment(entryId: String, content: String, originalContent: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val evolution = aiManager.analyzeEvolution(originalContent, content)
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val encrypted = encryptionManager.encryptText(content)

                val amendment = AmendmentEntity(
                    entryId = entryId,
                    encryptedContent = encrypted,
                    ageAtAmendment = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    aiEvolution = evolution
                )
                offlineEntryDao.insertAmendment(amendment)
            } catch (_: Exception) { }
        }
    }

    private fun AmendmentEntity.toDomain(): PhoenXAmendment {
        val ageJson = JSONObject(ageAtAmendment)
        return PhoenXAmendment(
            id = id,
            encryptedContent = encryptedContent,
            ageAtAmendment = AgeSnapshot(ageJson.getInt("years"), ageJson.getInt("months"), ageJson.getInt("days")),
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager, amendments: List<PhoenXAmendment>): PhoenXEntry {
        val decryptedText = encryptionManager.decryptText(encryptedPayload)
        
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.getInt("years"),
            months = ageJson.getInt("months"),
            days = ageJson.getInt("days")
        )

        return PhoenXEntry(
            id = id,
            ageAtCreation = age,
            encryptedContent = decryptedText.toByteArray(),
            type = try { EntryType.valueOf(entryType) } catch(e: Exception) { EntryType.THOUGHT },
            isYoungSelfLetter = isYoungSelfLetter,
            targetAge = targetAge,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary,
            aiTags = aiTags.split(",").filter { it.isNotEmpty() },
            amendments = amendments,
            temporalEvolution = if (amendments.isNotEmpty()) {
                // On récupère l'évolution depuis Room si disponible
                // (Ici on utilise une logique simplifiée pour le lien)
                "Évolution stylistique détectée par l'IA"
            } else null,
            hasEnigma = enigmaQuestion != null,
            scheduledDate = scheduledTimestamp?.let { Instant.ofEpochMilli(it) }
        )
    }
}

data class FilUiState(
    val entries: List<PhoenXEntry> = emptyList(),
    val totalCount: Int = 0,
    val minAge: Int = 0,
    val maxAge: Int = 0,
    val isLoading: Boolean = false
)
