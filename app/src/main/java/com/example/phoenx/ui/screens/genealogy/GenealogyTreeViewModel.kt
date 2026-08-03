package com.example.phoenx.ui.screens.genealogy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaDao
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.data.sync.toPersonEntity
import com.example.phoenx.domain.genealogy.TreeAlgorithm
import com.example.phoenx.domain.model.TreeLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GenealogyTreeViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val personMediaDao: PersonMediaDao,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    private val functions: com.google.firebase.functions.FirebaseFunctions,
    private val mediaManager: com.example.phoenx.data.media.MediaManager
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)

    // Cache des URLs résolues (Id du média/personne -> URL signée ou locale)
    private val _resolvedUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val resolvedUrls: StateFlow<Map<String, String>> = _resolvedUrls.asStateFlow()

    val allPersons: StateFlow<List<PersonEntity>> = _targetCreatorId.flatMapLatest { targetId ->
        android.util.Log.d("GenealogySecurityDebug", "allPersons query: targetId='$targetId', currentUser.uid='${auth.currentUser?.uid}'")
        if (targetId == null || targetId == auth.currentUser?.uid) {
            offlineEntryDao.getAllPersons()
        } else {
            // Lecture Firestore directe pour les Destinataires (v9.4.22)
            callbackFlow {
                android.util.Log.d("GenealogySecurityDebug", "Initiating Firestore listen for users/$targetId/persons")
                val listener = db.collection("users").document(targetId)
                    .collection("persons")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("GenealogySecurityDebug", "Firestore listen error: ${error.message}")
                        }
                        val list = snapshot?.documents?.map { doc ->
                            doc.toPersonEntity().copy(
                                parentIds = doc.getString("parentIds") ?: "",
                                isDeceased = doc.getBoolean("isDeceased") ?: false,
                                biography = doc.getString("biography") ?: ""
                            )
                        } ?: emptyList<PersonEntity>()
                        trySend(list)
                        
                        // Déclenche la résolution des avatars
                        resolveAvatars(targetId, list)
                    }
                awaitClose { listener.remove() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun resolveAvatars(creatorId: String, persons: List<PersonEntity>) {
        persons.forEach { person ->
            if (!person.imagePath.isNullOrBlank()) {
                resolveSingleUrl(creatorId, "persons", person.id, person.imagePath)
            }
        }
    }

    private fun resolveSingleUrl(creatorId: String, docType: String, docId: String, path: String, personId: String? = null) {
        if (_resolvedUrls.value.containsKey(docId)) return
        
        viewModelScope.launch {
            try {
                if (creatorId == auth.currentUser?.uid) {
                    // Mode Créateur : Résolution locale sécurisée
                    val url = mediaManager.getSafeUrl(path)
                    if (url != null) _resolvedUrls.update { it + (docId to url) }
                } else {
                    // Mode Destinataire : Appel Cloud Function v9.4.22
                    val params = mutableMapOf(
                        "creatorId" to creatorId,
                        "docType" to docType,
                        "docId" to docId
                    )
                    if (personId != null) params["personId"] = personId
                    
                    val result = functions.getHttpsCallable("getInheritedFileUrl").call(params).await()
                    val data = result.data as? Map<*, *>
                    val url = data?.get("url") as? String
                    if (url != null) _resolvedUrls.update { it + (docId to url) }
                }
            } catch (e: Exception) {
                android.util.Log.e("GenealogyVM", "Erreur résolution URL $docId", e)
            }
        }
    }

    fun loadTree(creatorId: String?) {
        _targetCreatorId.value = creatorId
        // Si mode Créateur, on pré-résout les avatars locaux
        if (creatorId == null || creatorId == auth.currentUser?.uid) {
            viewModelScope.launch {
                allPersons.first().forEach { person ->
                    if (!person.imagePath.isNullOrBlank()) {
                        resolveSingleUrl(auth.currentUser?.uid ?: "", "persons", person.id, person.imagePath)
                    }
                }
            }
        }
    }

    /**
     * Calcul du layout pour le rendu visuel (v9.4.22)
     */
    val treeLayout: StateFlow<TreeLayout> = combine(allPersons, _resolvedUrls) { persons, urls ->
        val resolved = persons.map { it.toResolvedPerson(urls[it.id]) }
        TreeAlgorithm.calculateLayout(resolved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TreeLayout(emptyList(), emptyList()))

    /**
     * Reconstruit la hiérarchie en mémoire (v9.4.22)
     * On cherche les "racines" (ceux qui n'ont pas de parents enregistrés)
     */
    val rootPersons: StateFlow<List<PersonEntity>> = allPersons.map { list ->
        list.filter { it.parentIds.isBlank() || it.parentIds == ",," }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Récupère les enfants d'une personne donnée (Reverse Lookup)
     */
    fun getChildrenOf(personId: String): Flow<List<PersonEntity>> {
        return offlineEntryDao.getChildrenOf(personId)
    }

    /**
     * Ajoute un lien de parenté (v9.4.22)
     * @param parentId L'ID du parent à ajouter
     * @param childId L'ID de la personne qui reçoit ce parent
     */
    fun linkParent(parentId: String, childId: String) {
        viewModelScope.launch {
            val child = allPersons.value.find { it.id == childId } ?: return@launch
            val currentParents = child.parentIds.trim(',').split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            
            if (!currentParents.contains(parentId) && currentParents.size < 2) {
                currentParents.add(parentId)
                val newCsv = "," + currentParents.joinToString(",") + ","
                val updatedChild = child.copy(parentIds = newCsv, syncStatus = "pending")
                offlineEntryDao.insertPerson(updatedChild)
            }
        }
    }

    fun updateBiography(personId: String, bio: String) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(biography = bio, syncStatus = "pending")
            offlineEntryDao.insertPerson(updated)
        }
    }

    fun toggleDeceased(personId: String) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(isDeceased = !person.isDeceased, syncStatus = "pending")
            offlineEntryDao.insertPerson(updated)
        }
    }

    fun createAndLinkPerson(firstName: String, lastName: String?, parentIds: List<String>) {
        viewModelScope.launch {
            val parentCsv = if (parentIds.isEmpty()) "" else "," + parentIds.joinToString(",") + ","
            val newPerson = PersonEntity(
                firstName = firstName,
                lastName = lastName,
                parentIds = parentCsv,
                syncStatus = "pending"
            )
            offlineEntryDao.insertPerson(newPerson)
        }
    }

    fun updatePersonIdentity(personId: String, firstName: String, lastName: String?, parentIds: List<String>) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val parentCsv = if (parentIds.isEmpty()) "" else "," + parentIds.joinToString(",") + ","
            val updated = person.copy(
                firstName = firstName,
                lastName = lastName,
                parentIds = parentCsv,
                syncStatus = "pending"
            )
            offlineEntryDao.insertPerson(updated)
        }
    }

    // --- GESTION MÉDIAS ---

    fun getMediaForPerson(personId: String): Flow<List<PersonMediaEntity>> {
        return personMediaDao.getMediaForPerson(personId)
    }

    fun addMedia(personId: String, path: String, type: String) {
        viewModelScope.launch {
            val media = PersonMediaEntity(
                personId = personId,
                mediaPath = path,
                mediaType = type,
                syncStatus = "pending"
            )
            personMediaDao.insertMedia(media)
        }
    }

    fun removeMedia(media: PersonMediaEntity) {
        viewModelScope.launch {
            personMediaDao.deleteMedia(media)
        }
    }

    fun resolveMediaUrl(personId: String, media: PersonMediaEntity) {
        val creatorId = _targetCreatorId.value ?: auth.currentUser?.uid ?: return
        resolveSingleUrl(creatorId, "personMedia", media.id, media.mediaPath, personId)
    }
}
