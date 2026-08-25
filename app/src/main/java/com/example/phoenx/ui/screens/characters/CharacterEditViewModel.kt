package com.example.phoenx.ui.screens.characters

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.sync.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineEntryDao: OfflineEntryDao,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _character = MutableStateFlow<PersonEntity?>(null)
    val character: StateFlow<PersonEntity?> = _character.asStateFlow()

    private val _isSaved = MutableSharedFlow<Boolean>()
    val isSaved = _isSaved.asSharedFlow()

    private val _appearanceCount = MutableStateFlow(0)
    val appearanceCount: StateFlow<Int> = _appearanceCount.asStateFlow()

    fun loadCharacter(personId: String) {
        viewModelScope.launch {
            val persons = offlineEntryDao.getPersonsByIds(listOf(personId))
            val person = persons.firstOrNull()
            _character.value = person
            
            if (person != null) {
                val entries = offlineEntryDao.getAllEntriesSync()
                _appearanceCount.value = entries.count { it.personIds.split(",").filter { it.isNotBlank() }.map { it.trim() }.contains(personId) }
            }
        }
    }

    fun deleteCharacter(personId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Nettoyage des références dans les souvenirs Room
                val allEntries = offlineEntryDao.getAllEntriesSync()
                allEntries.forEach { entry ->
                    val ids = entry.personIds.split(",").filter { it.isNotBlank() }.map { it.trim() }
                    if (ids.contains(personId)) {
                        val newIds = ids.filter { it != personId }.joinToString(",")
                        val finalCsv = if (newIds.isEmpty()) "" else ",$newIds,"
                        offlineEntryDao.updateEntryPersons(finalCsv, entry.id)
                    }
                }

                // 2. Suppression locale Room
                _character.value?.let { offlineEntryDao.deletePerson(it) }

                // 3. Suppression Cloud Firestore
                db.collection("users").document(userId)
                    .collection("persons").document(personId)
                    .delete()
                    .await()

                // 4. Suppression Cloud Storage
                try {
                    storage.reference.child("users").child(userId).child("cameos").child("$personId.jpg").delete().await()
                } catch (e: Exception) {
                    // Peut échouer si pas de photo, on ignore
                }

                _isSaved.emit(true)
            } catch (e: Exception) {
                android.util.Log.e("CharacterEdit", "Erreur suppression", e)
            }
        }
    }

    fun updateCharacter(
        firstName: String,
        lastName: String?,
        relationship: String?,
        distinctionType: String?,
        distinctionValue: String?,
        imageUri: Uri?,
        height: Int?,
        weight: Int?,
        eyeColor: String?,
        hairColor: String?,
        clothingStyle: String?,
        profession: String?,
        hasChildren: Boolean?,
        relationshipDetail: String?,
        characterType: String = "HUMAN"
    ) {
        val current = _character.value ?: return
        viewModelScope.launch {
            var finalImagePath = current.imagePath
            
            // Si une nouvelle image est fournie
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
                } catch (e: Exception) {
                    android.util.Log.e("CharacterEdit", "Erreur sauvegarde portrait", e)
                }
            }

            val updated = current.copy(
                firstName = firstName.trim(),
                lastName = lastName?.trim(),
                relationship = relationship,
                distinctionType = distinctionType,
                distinctionValue = distinctionValue,
                imagePath = finalImagePath,
                syncStatus = "pending", // Force re-sync
                height = height,
                weight = weight,
                eyeColor = eyeColor,
                hairColor = hairColor,
                clothingStyle = clothingStyle,
                profession = profession,
                hasChildren = hasChildren,
                relationshipDetail = relationshipDetail,
                characterType = characterType
            )

            offlineEntryDao.insertPerson(updated)
            
            // Déclenchement Sync
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
            
            _isSaved.emit(true)
        }
    }
}
