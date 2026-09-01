package com.example.phoenx.ui.screens.encounters

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toFirestoreMap
import com.example.phoenx.data.local.PersonMediaDao
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.data.sync.toPersonEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class EncounterViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val personMediaDao: PersonMediaDao, // v9.6.0
    private val mediaManager: com.example.phoenx.data.media.MediaManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _contextFilter = MutableStateFlow("Tous")
    val contextFilter: StateFlow<String> = _contextFilter.asStateFlow()

    private val _groupingMode = MutableStateFlow("Âge")
    val groupingMode: StateFlow<String> = _groupingMode.asStateFlow()

    // Liste des rencontres (Filtre Room v9.5.0)
    private val _encounterPersons = MutableStateFlow<List<PersonEntity>>(emptyList())
    val encounterPersons: StateFlow<List<PersonEntity>> = _encounterPersons.asStateFlow()

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    init {
        // Chargement local par défaut
        viewModelScope.launch {
            offlineEntryDao.getEncounterPersons().collect {
                _encounterPersons.value = it
            }
        }
    }

    /**
     * Charge les rencontres d'un héritage (v9.6.0)
     */
    fun loadRemoteEncounters(creatorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(creatorId)
                    .collection("persons")
                    .whereGreaterThan("categories", "") // v9.6.0: Filtrage catégories non vides
                    .get().await()
                
                val list = snapshot.documents.map { it.toPersonEntity() }
                    .filter { it.categories.contains(",ENCOUNTER,") }
                    .filter { it.visibility != "PRIVATE" } // v9.6.5 : Sécurité Jardin Secret
                
                _encounterPersons.value = list
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur chargement distant: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Liste filtrée et ordonnée pour la galerie (v9.6.0)
    val filteredEncounters: StateFlow<List<PersonEntity>> = combine(
        encounterPersons,
        _searchQuery,
        _contextFilter
    ) { list, query, filter ->
        list.filter { person ->
            val matchesQuery = person.firstName.contains(query, ignoreCase = true) || 
                             (person.lastName?.contains(query, ignoreCase = true) == true)
            
            val matchesFilter = when(filter) {
                "Tous" -> true
                "École" -> person.encounterContext == "SCHOOL"
                "Travail" -> person.encounterContext == "WORK"
                "Sport" -> person.encounterContext == "SPORT"
                "Passion" -> person.encounterContext == "PASSION"
                "Voyage" -> person.encounterContext == "TRAVEL"
                "Autre" -> person.encounterContext == "OTHER"
                else -> true
            }
            
            matchesQuery && matchesFilter
        }.sortedWith(compareBy({ it.encounterAge ?: Int.MAX_VALUE }, { it.firstName }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistiques pour l'en-tête (v9.6.0)
    data class EncounterStats(val total: Int, val minAge: Int?, val maxAge: Int?)
    val stats: StateFlow<EncounterStats> = encounterPersons.map { list ->
        val ages = list.mapNotNull { it.encounterAge }
        EncounterStats(
            total = list.size,
            minAge = ages.minOrNull(),
            maxAge = ages.maxOrNull()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EncounterStats(0, null, null))

    // Liste des contextes disponibles (pour les puces de filtre)
    val availableContexts: StateFlow<List<String>> = encounterPersons.map { list ->
        val contexts = list.mapNotNull { it.encounterContext }.distinct()
        val labels = mutableListOf("Tous")
        if (contexts.contains("SCHOOL")) labels.add("École")
        if (contexts.contains("WORK")) labels.add("Travail")
        if (contexts.contains("SPORT")) labels.add("Sport")
        if (contexts.contains("PASSION")) labels.add("Passion")
        if (contexts.contains("TRAVEL")) labels.add("Voyage")
        if (contexts.contains("OTHER")) labels.add("Autre")
        labels
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tous"))

    // Toutes les personnes pour le sélecteur "Présenté par"
    val allSelectablePersons: StateFlow<List<PersonEntity>> = offlineEntryDao.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateContextFilter(filter: String) {
        _contextFilter.value = filter
    }

    fun updateGroupingMode(mode: String) {
        _groupingMode.value = mode
    }

    /**
     * Récupère les médias liés à une personne (v9.6.0)
     */
    fun getMediaForPerson(personId: String): Flow<List<PersonMediaEntity>> {
        return personMediaDao.getMediaForPerson(personId)
    }

    /**
     * Calcule le nombre de souvenirs où la personne est citée (v9.6.0)
     */
    fun getMemoriesCountForPerson(personId: String): Flow<Int> {
        return offlineEntryDao.getAllEntries().map { list ->
            list.count { entry -> 
                entry.personIds.split(",").contains(personId)
            }
        }
    }

    /**
     * Retourne une personne par son ID, en gérant le local et le distant (v9.6.6)
     */
    fun getPersonById(personId: String, targetCreatorId: String?): Flow<PersonEntity?> {
        return if (targetCreatorId == null || targetCreatorId == auth.currentUser?.uid) {
            offlineEntryDao.getPersonByIdFlow(personId)
        } else {
            // Mode Héritier : On cherche dans la liste déjà chargée ou on observe le Flow
            encounterPersons.map { list -> list.find { it.id == personId } }
        }
    }

    /**
     * Sauvegarde atomique d'une rencontre (Room + Firestore)
     */
    fun saveEncounter(person: PersonEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                val cleanCategories = person.categories.split(",")
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(",", prefix = ",", postfix = ",")

                val finalPerson = person.copy(
                    firstName = person.firstName.trim(),
                    lastName = person.lastName?.trim(),
                    encounterBiography = person.encounterBiography.trim(),
                    encounterLocationLabel = person.encounterLocationLabel?.trim(),
                    linkNature = person.linkNature?.trim(),
                    encounterContextLabel = person.encounterContextLabel?.trim(),
                    relationEndReason = person.relationEndReason?.trim(),
                    categories = cleanCategories,
                    syncStatus = "pending"
                )

                offlineEntryDao.upsertPerson(finalPerson)

                val encounterPath = finalPerson.encounterImagePath
                val isLocalEncounterPath = !encounterPath.isNullOrBlank() && (encounterPath.startsWith("/data/") || !encounterPath.startsWith("users/"))
                val safeEncounterStorageUrl = if (isLocalEncounterPath) null else encounterPath

                val imagePath = finalPerson.imagePath
                val isLocalImagePath = !imagePath.isNullOrBlank() && (imagePath.startsWith("/data/") || !imagePath.startsWith("users/"))
                val safeImageStorageUrl = if (isLocalImagePath) null else imagePath

                db.collection("users").document(userId)
                    .collection("persons").document(finalPerson.id)
                    .set(finalPerson.toFirestoreMap(storageUrl = safeImageStorageUrl, encounterStorageUrl = safeEncounterStorageUrl))
                    .await()

                offlineEntryDao.upsertPerson(finalPerson.copy(syncStatus = "synced"))
                SyncWorker.trigger(context)
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur sauvegarde rencontre : ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retire la catégorie ENCOUNTER d'une personne.
     */
    fun removeEncounterCategory(person: PersonEntity) {
        viewModelScope.launch {
            // Nettoyage des références introducedById (Lot C)
            try {
                val allPersons = offlineEntryDao.getAllPersons().first()
                val personsToUpdate = allPersons.filter { it.introducedById == person.id }
                personsToUpdate.forEach { 
                    saveEncounter(it.copy(introducedById = null))
                }
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur nettoyage introducedById : ${e.message}")
            }

            val categories = person.categories.split(",").filter { it.isNotBlank() && it != "ENCOUNTER" }
            
            if (categories.isEmpty()) {
                deletePersonPermanently(person)
            } else {
                val updated = person.copy(
                    categories = categories.joinToString(",", prefix = ",", postfix = ",")
                )
                saveEncounter(updated)
            }
        }
    }

    private suspend fun deletePersonPermanently(person: PersonEntity) {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Suppression du portrait de Rencontre spécifique
        if (!person.encounterImagePath.isNullOrBlank()) {
            try {
                mediaManager.deleteFile(person.encounterImagePath)
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur suppression portrait Storage: ${e.message}")
            }
        }

        // 2. Nettoyage des médias secondaires
        try {
            val mediaList = personMediaDao.getMediaForPerson(person.id).first()
            mediaList.forEach { media ->
                // Suppression Storage
                try {
                    mediaManager.deleteFile(media.mediaPath)
                } catch (e: Exception) {
                    android.util.Log.e("EncounterVM", "Erreur suppression fichier media ${media.id}: ${e.message}")
                }
                
                // Suppression Firestore
                try {
                    db.collection("users").document(userId)
                        .collection("persons").document(person.id)
                        .collection("media").document(media.id)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    android.util.Log.e("EncounterVM", "Erreur suppression Firestore media ${media.id}: ${e.message}")
                }
                
                // Suppression Room
                try {
                    personMediaDao.deleteMedia(media)
                } catch (e: Exception) {
                    android.util.Log.e("EncounterVM", "Erreur suppression Room media ${media.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EncounterVM", "Erreur liste médias à supprimer: ${e.message}")
        }

        // 3. Suppression finale de la personne
        try {
            offlineEntryDao.deletePerson(person)
            db.collection("users").document(userId)
                .collection("persons").document(person.id).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("EncounterVM", "Erreur suppression finale : ${e.message}")
        }
    }

    /**
     * Supprime un média spécifique (Arbre ou Rencontre)
     */
    fun removeMedia(media: PersonMediaEntity) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            // 1. Suppression Storage
            try {
                mediaManager.deleteFile(media.mediaPath)
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur suppression fichier media ${media.id}: ${e.message}")
            }
            
            // 2. Suppression Firestore
            try {
                db.collection("users").document(userId)
                    .collection("persons").document(media.personId)
                    .collection("media").document(media.id)
                    .delete()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur suppression Firestore media ${media.id}: ${e.message}")
            }
            
            // 3. Suppression Room
            try {
                personMediaDao.deleteMedia(media)
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur suppression Room media ${media.id}: ${e.message}")
            }
        }
    }

    /**
     * Ajoute un média secondaire à une Rencontre
     */
    fun addMediaComplement(personId: String, file: java.io.File, type: String) {
        viewModelScope.launch {
            var thumbnailPath: String? = null
            
            if (type == "VIDEO") {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val bitmap = retriever.getFrameAtTime(0)
                    retriever.release()
                    
                    if (bitmap != null) {
                        val thumbFile = java.io.File(context.cacheDir, "thumb_${file.name}.jpg")
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        }
                        thumbnailPath = thumbFile.absolutePath
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EncounterVM", "Erreur vignette vidéo: ${e.message}")
                }
            }

            val media = PersonMediaEntity(
                personId = personId,
                mediaPath = file.absolutePath,
                mediaType = type,
                thumbnailPath = thumbnailPath,
                syncStatus = "pending"
            )
            android.util.Log.d("PHX_MEDIA_DEBUG", "EncounterViewModel addMediaComplement: personId=$personId, mediaId=${media.id}")
            personMediaDao.insertMedia(media)
            SyncWorker.trigger(context)
        }
    }

    fun setHeirKey(key: ByteArray?) {
        _heirKey.value = key
    }

    /**
     * Convertit une Uri en File pour l'upload d'image (Étape 3)
     */
    fun uriToFile(uri: android.net.Uri): java.io.File? {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType?.contains("video") == true) {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = java.io.File(context.cacheDir, "encounter_media_${java.util.UUID.randomUUID()}.mp4")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempFile
            } catch (e: Exception) {
                android.util.Log.e("EncounterVM", "Erreur copie vidéo: ${e.message}")
                null
            }
        } else {
            // v9.7.9 : Compression unifiée pour les photos (Modèle Souvenirs)
            return com.example.phoenx.ui.util.ImageUtils.compressAndResize(context, uri)
        }
    }
}
