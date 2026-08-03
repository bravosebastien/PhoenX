package com.example.phoenx.ui.screens.genealogy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaDao
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toPersonEntity
import com.example.phoenx.domain.genealogy.TreeAlgorithm
import com.example.phoenx.domain.model.TreeLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GenealogyTreeViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val personMediaDao: PersonMediaDao,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    private val functions: com.google.firebase.functions.FirebaseFunctions,
    private val mediaManager: com.example.phoenx.data.media.MediaManager,
    @ApplicationContext private val context: Context // v9.4.24
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
                            doc.toPersonEntity() // v9.4.24: Utilise le mapper centralisé complet
                        } ?: emptyList<PersonEntity>()
                        trySend(list)
                        
                        // Déclenche la résolution des avatars
                        resolveAvatars(targetId, list)
                    }
                awaitClose { listener.remove() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observation réactive pour le fallback photo (Point 4)
        combine(allPersons, personMediaDao.getAllMedia()) { persons, media ->
            val creatorId = _targetCreatorId.value ?: auth.currentUser?.uid ?: return@combine
            persons.forEach { person ->
                if (person.imagePath.isNullOrBlank()) {
                    // Fallback sur la galerie si pas de photo de profil
                    val firstMedia = media.find { it.personId == person.id }
                    if (firstMedia != null) {
                        resolveSingleUrl(creatorId, "personMedia", person.id, firstMedia.mediaPath, person.id)
                    }
                } else {
                    // Photo de profil dédiée
                    resolveSingleUrl(creatorId, "persons", person.id, person.imagePath)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun resolveAvatars(creatorId: String, persons: List<PersonEntity>) {
        persons.forEach { person ->
            if (!person.imagePath.isNullOrBlank()) {
                resolveSingleUrl(creatorId, "persons", person.id, person.imagePath)
            }
        }
    }

    private fun resolveSingleUrl(creatorId: String, docType: String, docId: String, path: String, personId: String? = null) {
        // v9.4.24: On ne bloque que si l'URL est déjà une URL distante (http/https)
        // pour permettre la mise à jour chemin local -> URL Storage
        val current = _resolvedUrls.value[docId]
        if (current != null && current.startsWith("http")) return
        
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
        // On récupère le premier média de galerie pour ceux qui n'ont pas de photo de profil
        val resolved = persons.map { person ->
            var finalPhotoUrl = urls[person.id]
            
            // Si pas d'avatar, on pourrait chercher dans urls par média ID mais on ne sait pas lesquels sont liés.
            // Pour l'instant, on assure déjà que l'avatar s'affiche avec file://
            
            person.toResolvedPerson(finalPhotoUrl) 
        }
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
                SyncWorker.trigger(context) // v9.4.24
            }
        }
    }

    fun updateBiography(personId: String, bio: String) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(biography = bio, syncStatus = "pending")
            offlineEntryDao.insertPerson(updated)
            SyncWorker.trigger(context) // v9.4.24
        }
    }

    fun toggleDeceased(personId: String) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(isDeceased = !person.isDeceased, syncStatus = "pending")
            offlineEntryDao.insertPerson(updated)
            SyncWorker.trigger(context) // v9.4.24
        }
    }

    fun createAndLinkPerson(firstName: String, lastName: String?, parentIds: List<String>, childrenIdsToLink: List<String> = emptyList()) {
        viewModelScope.launch {
            val parentCsv = if (parentIds.isEmpty()) "" else "," + parentIds.joinToString(",") + ","
            val newPerson = PersonEntity(
                firstName = firstName,
                lastName = lastName,
                parentIds = parentCsv,
                syncStatus = "pending"
            )
            offlineEntryDao.insertPerson(newPerson)
            
            // Si on créait un ascendant ou un co-parent, on lie les enfants au nouveau parent (v9.4.23)
            childrenIdsToLink.forEach { childId ->
                linkParent(newPerson.id, childId)
            }
            SyncWorker.trigger(context) // v9.4.24
        }
    }

    fun updatePersonIdentity(personId: String, firstName: String, lastName: String?, parentIds: List<String>, relationLabel: String? = null) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val parentCsv = if (parentIds.isEmpty()) "" else "," + parentIds.joinToString(",") + ","
            val updated = person.copy(
                firstName = firstName,
                lastName = lastName,
                parentIds = parentCsv,
                reparentedRelationLabel = relationLabel,
                syncStatus = "pending"
            )
            offlineEntryDao.insertPerson(updated)
        }
    }

    /**
     * Enregistre l'intégralité des détails modifiables d'une personne (v9.4.24)
     */
    fun savePersonDetails(
        personId: String,
        biography: String,
        reparentedRelationLabel: String?,
        isDeceased: Boolean,
        imagePath: String?
    ) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(
                biography = biography,
                reparentedRelationLabel = reparentedRelationLabel,
                isDeceased = isDeceased,
                imagePath = imagePath,
                syncStatus = "pending"
            )
            offlineEntryDao.insertPerson(updated)
            
            // Sync immédiate Firestore (v9.4.24: SANS imagePath local)
            val updates = mutableMapOf<String, Any?>(
                "biography" to biography,
                "reparentedRelationLabel" to reparentedRelationLabel,
                "isDeceased" to isDeceased
            )
            db.collection("users").document(auth.currentUser?.uid ?: "")
                .collection("persons").document(personId)
                .update(updates)
            
            SyncWorker.trigger(context) // v9.4.24
        }
    }

    fun deletePerson(personId: String, childRelationLabels: Map<String, String> = emptyMap()) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Snapshot actuel
                val currentPersons = allPersons.value
                val personToDelete = currentPersons.find { it.id == personId } ?: return@launch
                
                // 1. Identification des parents de P (Grands-parents des futurs orphelins)
                val grandparentIds = personToDelete.parentIds.trim(',').split(",").filter { it.isNotBlank() }

                // 2. Nettoyage et REMONTÉE des liens chez les enfants (v9.4.23)
                val children = currentPersons.filter { it.parentIds.contains(",$personId,") }
                
                children.forEach { child ->
                    val parentList = child.parentIds.trim(',').split(",").filter { it.isNotBlank() }.toMutableList()
                    parentList.remove(personId)
                    
                    // Si P avait des parents, on les injecte (remontée)
                    var hasBeenPromoted = false
                    grandparentIds.forEach { gpid ->
                        if (!parentList.contains(gpid) && parentList.size < 2) {
                            parentList.add(gpid)
                            hasBeenPromoted = true
                        }
                    }
                    
                    val newParentCsv = if (parentList.isEmpty()) "" else "," + parentList.joinToString(",") + ","
                    val customLabel = childRelationLabels[child.id]
                    
                    val updatedChild = child.copy(
                        parentIds = newParentCsv,
                        isReparented = hasBeenPromoted,
                        reparentedRelationLabel = customLabel ?: child.reparentedRelationLabel,
                        syncStatus = "pending"
                    )
                    
                    offlineEntryDao.insertPerson(updatedChild)
                    
                    // Mise à jour Firestore immédiate
                    val updates = mutableMapOf<String, Any?>(
                        "parentIds" to newParentCsv,
                        "isReparented" to hasBeenPromoted,
                        "reparentedRelationLabel" to (customLabel ?: child.reparentedRelationLabel)
                    )
                    db.collection("users").document(userId)
                        .collection("persons").document(child.id)
                        .update(updates)
                }

                // 3. Suppression Firestore du document principal
                db.collection("users").document(userId)
                    .collection("persons").document(personId)
                    .delete()
                    .await()

                SyncWorker.trigger(context) // v9.4.24

                // 4. Suppression des médias associés (Fichiers + Room + Firestore)
                val mediaList = personMediaDao.getMediaForPerson(personId).first()
                mediaList.forEach { media ->
                    try {
                        val file = File(media.mediaPath)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {}
                    personMediaDao.deleteMedia(media)
                    
                    // Suppression média Firestore
                    db.collection("users").document(userId)
                        .collection("persons").document(personId)
                        .collection("media").document(media.id)
                        .delete()
                }

                // 5. Suppression finale de la personne (Room)
                offlineEntryDao.deletePerson(personToDelete)
                // Suppression du portrait Cameo local
                if (!personToDelete.imagePath.isNullOrBlank()) {
                    try {
                        File(personToDelete.imagePath).delete()
                    } catch (e: Exception) {}
                }

            } catch (e: Exception) {
                android.util.Log.e("GenealogyVM", "Erreur lors de la suppression de la personne $personId", e)
            }
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
            SyncWorker.trigger(context) // v9.4.24
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
