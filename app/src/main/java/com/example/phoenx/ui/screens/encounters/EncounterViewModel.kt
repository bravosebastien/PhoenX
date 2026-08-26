package com.example.phoenx.ui.screens.encounters

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toFirestoreMap
import com.example.phoenx.data.sync.toPersonEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EncounterViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Liste des rencontres (Filtre Room v9.5.0)
    val encounterPersons: StateFlow<List<PersonEntity>> = offlineEntryDao.getEncounterPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toutes les personnes pour le sélecteur "Présenté par" (Famille + Rencontres)
    val allSelectablePersons: StateFlow<List<PersonEntity>> = offlineEntryDao.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Layout du graphe (Calculé réactivement v9.5.1 - ÉTAPE A)
    val graphLayout: StateFlow<EncounterLayout> = encounterPersons.map { encounters ->
        EncounterGraphAlgorithm.calculateLayout(encounters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EncounterLayout(emptyList(), 0f))

    /**
     * Sauvegarde atomique d'une rencontre (Room + Firestore)
     */
    fun saveEncounter(person: PersonEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // 1. Nettoyage CSV des catégories (Garantie de format strict)
                val cleanCategories = person.categories.split(",")
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(",", prefix = ",", postfix = ",")

                // 2. Nettoyage des libellés (Règle .trim() systématique)
                val finalPerson = person.copy(
                    firstName = person.firstName.trim(),
                    lastName = person.lastName?.trim(),
                    biography = person.biography.trim(),
                    encounterLocationLabel = person.encounterLocationLabel?.trim(),
                    linkNature = person.linkNature?.trim(),
                    categories = cleanCategories,
                    syncStatus = "pending"
                )

                // 3. Persistance Room (Locale)
                offlineEntryDao.insertPerson(finalPerson)

                // 4. Persistance Firestore (Cloud) - On écrase l'objet complet
                db.collection("users").document(userId)
                    .collection("persons").document(finalPerson.id)
                    .set(finalPerson.toFirestoreMap())
                    .await()

                // 5. Marquage synchro
                offlineEntryDao.insertPerson(finalPerson.copy(syncStatus = "synced"))
                
                // Déclenchement SyncWorker pour les portraits si nécessaire
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
     * Si elle n'a plus d'autres catégories, elle est supprimée de la base.
     */
    fun removeEncounterCategory(person: PersonEntity) {
        viewModelScope.launch {
            val categories = person.categories.split(",").filter { it.isNotBlank() && it != "ENCOUNTER" }
            
            if (categories.isEmpty()) {
                // Suppression réelle si plus aucune catégorie (avec confirmation UI normalement)
                deletePersonPermanently(person)
            } else {
                // Mise à jour : on retire juste ENCOUNTER
                val updated = person.copy(
                    categories = categories.joinToString(",", prefix = ",", postfix = ",")
                )
                saveEncounter(updated)
            }
        }
    }

    private suspend fun deletePersonPermanently(person: PersonEntity) {
        val userId = auth.currentUser?.uid ?: return
        try {
            offlineEntryDao.deletePerson(person)
            db.collection("users").document(userId)
                .collection("persons").document(person.id).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("EncounterVM", "Erreur suppression : ${e.message}")
        }
    }
}
