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
import com.example.phoenx.domain.model.VisualGroup
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

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    val allPersons: StateFlow<List<PersonEntity>> = _targetCreatorId.flatMapLatest { targetId ->
        android.util.Log.d("GenealogySecurityDebug", "allPersons query: targetId='$targetId', currentUser.uid='${auth.currentUser?.uid}'")
        if (targetId == null || targetId == auth.currentUser?.uid) {
            // MODE CRÉATEUR : Filtrage par catégorie FAMILY ou vide (Garde-fou)
            offlineEntryDao.getAllPersons().map { list ->
                list.filter { p -> 
                    val hasFamilyCat = p.categories.contains(",FAMILY,")
                    val isOldFamilyData = p.categories.isBlank() || p.categories == ",,"
                    val hasFamilyMarkers = p.parentIds.replace(",", "").isNotBlank() || p.isDeceased
                    
                    hasFamilyCat || (isOldFamilyData && hasFamilyMarkers)
                }
            }
        } else {
            // MODE DESTINATAIRE (Rattrapage v9.4.27)
            callbackFlow {
                val listener = db.collection("users").document(targetId)
                    .collection("persons")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("GenealogySecurityDebug", "Erreur snapshot persons: ${error.message}")
                            return@addSnapshotListener
                        }
                        val list = snapshot?.documents?.map { it.toPersonEntity() } ?: emptyList()
                        // v9.6.5 : Sécurité Jardin Secret + Filtrage Catégories (v12.2)
                        val filtered = list.filter { p -> 
                            val hasFamilyCat = p.categories.contains(",FAMILY,")
                            val isOldFamilyData = p.categories.isBlank() || p.categories == ",,"
                            val hasFamilyMarkers = p.parentIds.replace(",", "").isNotBlank() || p.isDeceased
                            
                            p.visibility != "PRIVATE" && (hasFamilyCat || (isOldFamilyData && hasFamilyMarkers))
                        }
                        android.util.Log.d("GenealogySecurityDebug", "Snapshot distant persons: count=${filtered.size}")
                        trySend(filtered)
                    }
                awaitClose { listener.remove() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Résolution d'une URL pour un média de l'Arbre (v9.4.17)
     */
    fun resolveSingleUrl(creatorId: String, docType: String, docId: String, path: String, personId: String? = null, fieldOverride: String? = null) {
        if (path.isBlank()) return
        if (_resolvedUrls.value.containsKey(docId)) return

        viewModelScope.launch {
            try {
                if (java.io.File(path).exists()) {
                    val localUrl = if (path.startsWith("file://")) path else "file://$path"
                    android.util.Log.d("TreePhotoDebug", "Updating Map (Local): id=$docId, oldSize=${_resolvedUrls.value.size}")
                    _resolvedUrls.update { it + (docId to localUrl) }
                    android.util.Log.d("TreePhotoDebug", "Updated Map (Local): id=$docId, newSize=${_resolvedUrls.value.size}")
                } else {
                    // Résolution Storage
                    val url = mediaManager.getSafeUrl(
                        pathOrUrl = path,
                        creatorId = if (creatorId != auth.currentUser?.uid) creatorId else null,
                        docType = docType,
                        docId = docId,
                        field = fieldOverride ?: (if (docType == "persons") "imageUrl" else "mediaPath"),
                        personId = personId
                    )
                    if (url != null) {
                        android.util.Log.d("PHOENX_TREE_TRACE", "SUCCÈS résolution $docId: $url")
                        android.util.Log.d("TreePhotoDebug", "Updating Map (Remote): id=$docId, oldSize=${_resolvedUrls.value.size}")
                        _resolvedUrls.update { it + (docId to url) }
                        android.util.Log.d("TreePhotoDebug", "Updated Map (Remote): id=$docId, newSize=${_resolvedUrls.value.size}")
                    } else {
                        android.util.Log.w("PHOENX_TREE_TRACE", "URL Nulle retournée pour $docId")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_TREE_TRACE", "ERREUR résolution URL $docId: ${e.message}", e)
            }
        }
    }

    fun loadTree(creatorId: String?) {
        _targetCreatorId.value = creatorId
        // Si mode Créateur, on pré-résout les avatars locaux en observant le flux (v9.6.7)
        if (creatorId == null || creatorId == auth.currentUser?.uid) {
            viewModelScope.launch {
                allPersons.collectLatest { persons ->
                    persons.forEach { person ->
                        val bestPath = person.encounterImagePath ?: person.imagePath
                        if (!bestPath.isNullOrBlank()) {
                            val field = if (person.encounterImagePath != null) "encounterImagePath" else "imageUrl"
                            resolveSingleUrl(auth.currentUser?.uid ?: "", "persons", person.id, bestPath, fieldOverride = field)
                        }
                    }
                }
            }
        }
    }

    fun setHeirKey(key: ByteArray?) {
        _heirKey.value = key
    }

    /**
     * Calcul du layout pour le rendu visuel (v9.4.22)
     */
    val treeLayout: StateFlow<TreeLayout> = combine(allPersons, _resolvedUrls) { persons, urls ->
        android.util.Log.d("TreePhotoDebug", "Recalculating TreeLayout: persons=${persons.size}, resolvedUrls=${urls.size}")
        // On récupère le premier média de galerie pour ceux qui n'ont pas de photo de profil
        val resolved = persons.map { person ->
            var finalPhotoUrl = urls[person.id]
            person.toResolvedPerson(finalPhotoUrl) 
        }
        TreeAlgorithm.calculateLayout(resolved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TreeLayout(emptyList(), emptyList()))

    /**
     * Reconstruit la hiérarchie par GROUPES pour la vue Liste (v9.4.26)
     */
    val treeGroups: StateFlow<List<VisualGroup>> = treeLayout.map { layout ->
        val nodes = layout.nodes
        val groups = mutableListOf<VisualGroup>()
        val processedNodeIds = mutableSetOf<String>()

        // Groupement par couples et niveaux
        nodes.forEach { node ->
            if (!processedNodeIds.contains(node.person.id)) {
                // Trouver le partenaire éventuel
                val spouseId = layout.coupleConnections.find { it.first == node.person.id }?.second
                    ?: layout.coupleConnections.find { it.second == node.person.id }?.first
                
                val members = mutableListOf(node.person)
                processedNodeIds.add(node.person.id)
                
                if (spouseId != null) {
                    layout.nodes.find { it.person.id == spouseId }?.let {
                        members.add(it.person)
                        processedNodeIds.add(it.person.id)
                    }
                }

                groups.add(VisualGroup(
                    id = "group_${node.person.id}",
                    level = node.generation,
                    members = members,
                    children = emptyList()
                ))
            }
        }

        // Attribution des enfants à chaque groupe
        val finalGroups = groups.map { group ->
            val parentIds = group.members.map { it.id }
            val childrenNodeIds = layout.connections
                .filter { it.first.any { pid -> parentIds.contains(pid) } }
                .map { it.second }
            
            group.copy(children = groups.filter { childGroup -> 
                childGroup.members.any { childrenNodeIds.contains(it.id) }
            })
        }

        // On ne retourne que les racines (level 0) pour la récursion UI
        finalGroups.filter { it.level == 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Récupère les médias d'une personne avec résolution automatique (v9.4.17)
     */
    fun getMediaForPerson(personId: String): Flow<List<PersonMediaEntity>> {
        val targetId = _targetCreatorId.value
        val currentUid = auth.currentUser?.uid ?: ""
        
        return if (targetId == null || targetId == currentUid) {
            personMediaDao.getMediaForPerson(personId).onEach { list ->
                // v9.6.7 : Pré-résolution réactive uniquement pour les nouveaux IDs
                val currentCache = _resolvedUrls.value
                list.forEach { media ->
                    val path = media.thumbnailPath ?: media.mediaPath
                    val cacheKey = if (media.thumbnailPath != null) "thumb_${media.id}" else media.id
                    
                    if (!currentCache.containsKey(cacheKey) && !path.isNullOrBlank()) {
                        val field = if (media.thumbnailPath != null) "thumbnailPath" else "mediaPath"
                        resolveSingleUrl(currentUid, "personMedia", cacheKey, path, personId, fieldOverride = field)
                    }
                }
            }
        } else {
            callbackFlow {
                val listener = db.collection("users").document(targetId)
                    .collection("persons").document(personId)
                    .collection("media")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("GenealogySecurityDebug", "Erreur snapshot media: ${error.message}")
                            return@addSnapshotListener
                        }
                        val list = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                PersonMediaEntity(
                                    id = doc.id,
                                    personId = personId,
                                    mediaPath = doc.getString("mediaPath") ?: "",
                                    mediaType = doc.getString("mediaType") ?: "PHOTO",
                                    thumbnailPath = doc.getString("thumbnailPath"),
                                    syncStatus = "synced"
                                )
                            } catch (e: Exception) { null }
                        } ?: emptyList()
                        
                        // Déclenche la résolution des URLs pour les nouveaux médias
                        list.forEach { media ->
                            resolveSingleUrl(targetId, "personMedia", media.id, media.mediaPath, personId)
                            if (media.thumbnailPath != null) {
                                resolveSingleUrl(targetId, "personMedia", "thumb_${media.id}", media.thumbnailPath, personId)
                            }
                        }
                        
                        trySend(list)
                    }
                awaitClose { listener.remove() }
            }
        }
    }

    /**
     * Ajoute un média à la galerie d'une personne.
     */
    fun addMedia(personId: String, file: File, type: String) {
        viewModelScope.launch {
            var thumbnailPath: String? = null
            
            if (type == "VIDEO") {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val bitmap = retriever.getFrameAtTime(0)
                    retriever.release()
                    
                    if (bitmap != null) {
                        val thumbFile = File(context.cacheDir, "thumb_${file.name}.jpg")
                        java.io.FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                        }
                        thumbnailPath = thumbFile.absolutePath
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GenealogyVM", "Erreur vignette vidéo: ${e.message}")
                }
            }

            val media = PersonMediaEntity(
                personId = personId,
                mediaPath = file.absolutePath,
                mediaType = type,
                thumbnailPath = thumbnailPath,
                syncStatus = "pending"
            )
            personMediaDao.insertMedia(media)
            SyncWorker.trigger(context) // v9.4.24
        }
    }

    fun removeMedia(media: PersonMediaEntity) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            // 1. Suppression Storage
            try {
                mediaManager.deleteFile(media.mediaPath)
            } catch (e: Exception) {
                android.util.Log.e("GenealogyVM", "Erreur suppression fichier media ${media.id}: ${e.message}")
            }
            
            // 2. Suppression Firestore
            try {
                db.collection("users").document(userId)
                    .collection("persons").document(media.personId)
                    .collection("media").document(media.id)
                    .delete()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("GenealogyVM", "Erreur suppression Firestore media ${media.id}: ${e.message}")
            }
            
            // 3. Suppression Room
            try {
                personMediaDao.deleteMedia(media)
            } catch (e: Exception) {
                android.util.Log.e("GenealogyVM", "Erreur suppression Room media ${media.id}: ${e.message}")
            }
        }
    }

    fun savePersonDetails(personId: String, biography: String, relationLabel: String?, isDeceased: Boolean, photoPath: String?) {
        viewModelScope.launch {
            val persons = offlineEntryDao.getPersonsByIds(listOf(personId))
            if (persons.isNotEmpty()) {
                val updated = persons.first().copy(
                    biography = biography,
                    reparentedRelationLabel = relationLabel,
                    isDeceased = isDeceased,
                    imagePath = photoPath,
                    syncStatus = "pending"
                )
                offlineEntryDao.upsertPerson(updated)
                SyncWorker.trigger(context)
            }
        }
    }

    fun deletePerson(personId: String, reparentedLabels: Map<String, String>) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val person = offlineEntryDao.getPersonsByIds(listOf(personId)).firstOrNull() ?: return@launch
            
            // 1. Gérer le re-parentage des enfants (Continuité de l'arbre)
            val children = offlineEntryDao.getChildrenOf(personId).first()
            val parents = person.parentIds.trim(',').split(",").filter { it.isNotBlank() }
            
            children.forEach { child ->
                val currentParents = child.parentIds.trim(',').split(",").filter { it.isNotBlank() && it != personId }
                val newParents = (currentParents + parents).distinct()
                
                val updatedChild = child.copy(
                    parentIds = if (newParents.isEmpty()) "" else "," + newParents.joinToString(",") + ",",
                    isReparented = parents.isNotEmpty(), // Marqueur visuel si rattaché aux grands-parents
                    reparentedRelationLabel = reparentedLabels[child.id],
                    syncStatus = "pending"
                )
                offlineEntryDao.upsertPerson(updatedChild)
            }

            // 2. Supprimer la personne (Local + Firestore)
            offlineEntryDao.deletePerson(person)
            db.collection("users").document(userId).collection("persons").document(personId).delete().await()
            
            // 3. Nettoyer les médias rattachés
            val mediaList = personMediaDao.getMediaForPerson(personId).first()
            mediaList.forEach { removeMedia(it) }
            
            SyncWorker.trigger(context)
        }
    }

    fun updatePersonIdentity(personId: String, firstName: String, lastName: String?, parentIds: List<String>) {
        viewModelScope.launch {
            val persons = offlineEntryDao.getPersonsByIds(listOf(personId))
            if (persons.isNotEmpty()) {
                val updated = persons.first().copy(
                    firstName = firstName,
                    lastName = lastName,
                    parentIds = "," + parentIds.joinToString(",") + ",",
                    syncStatus = "pending"
                )
                offlineEntryDao.upsertPerson(updated)
                SyncWorker.trigger(context)
            }
        }
    }

    fun createAndLinkPerson(firstName: String, lastName: String?, parentIds: List<String>, childrenIdsToLink: List<String> = emptyList()) {
        viewModelScope.launch {
            val newPerson = PersonEntity(
                firstName = firstName,
                lastName = lastName,
                parentIds = "," + parentIds.joinToString(",") + ",",
                categories = ",FAMILY,",
                syncStatus = "pending"
            )
            offlineEntryDao.upsertPerson(newPerson)
            
            // Lier aux enfants existants
            if (childrenIdsToLink.isNotEmpty()) {
                val children = offlineEntryDao.getPersonsByIds(childrenIdsToLink)
                children.forEach { child ->
                    val currentParents = child.parentIds.trim(',').split(",").filter { it.isNotBlank() }
                    if (!currentParents.contains(newPerson.id)) {
                        val updatedChild = child.copy(
                            parentIds = "," + (currentParents + newPerson.id).joinToString(",") + ",",
                            syncStatus = "pending"
                        )
                        offlineEntryDao.upsertPerson(updatedChild)
                    }
                }
            }
            
            SyncWorker.trigger(context)
        }
    }

    fun getChildrenOf(personId: String): Flow<List<PersonEntity>> {
        return offlineEntryDao.getChildrenOf(personId)
    }

    fun uriToFile(uri: android.net.Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            val extension = if (mimeType?.contains("video") == true) "mp4" else "jpg"

            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = java.io.File(context.cacheDir, "genealogy_media_${java.util.UUID.randomUUID()}.$extension")
            inputStream?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("GenealogyVM", "Erreur copie URI: ${e.message}")
            null
        }
    }
}
