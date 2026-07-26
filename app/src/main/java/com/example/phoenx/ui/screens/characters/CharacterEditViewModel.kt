package com.example.phoenx.ui.screens.characters

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _character = MutableStateFlow<PersonEntity?>(null)
    val character: StateFlow<PersonEntity?> = _character.asStateFlow()

    private val _isSaved = MutableSharedFlow<Boolean>()
    val isSaved = _isSaved.asSharedFlow()

    fun loadCharacter(personId: String) {
        viewModelScope.launch {
            val persons = offlineEntryDao.getPersonsByIds(listOf(personId))
            _character.value = persons.firstOrNull()
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
        relationshipDetail: String?
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
                firstName = firstName,
                lastName = lastName,
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
                relationshipDetail = relationshipDetail
            )

            offlineEntryDao.insertPerson(updated)
            
            // Déclenchement Sync
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
            
            _isSaved.emit(true)
        }
    }
}
