package com.example.phoenx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.NotificationContactEntity
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NotificationContactsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<NotificationContactEntity>>(emptyList())
    val contacts: StateFlow<List<NotificationContactEntity>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Écouter Room (Réactivité)
        viewModelScope.launch {
            offlineEntryDao.getAllNotificationContacts().collectLatest { list ->
                _contacts.value = list
            }
        }

        // 2. Synchroniser depuis Firestore
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot: QuerySnapshot = db.collection("users").document(userId)
                    .collection("notificationContacts")
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    val contact = NotificationContactEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: 0L
                    )
                    offlineEntryDao.insertNotificationContact(contact)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotifContactsVM", "Erreur sync contacts", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addContact(name: String, email: String, relationship: String) {
        val userId = auth.currentUser?.uid ?: return
        if (_contacts.value.size >= 2) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val now = com.google.firebase.Timestamp.now()
                val contactData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "relationship" to relationship,
                    "addedAt" to now
                )
                val docRef = db.collection("users").document(userId)
                    .collection("notificationContacts")
                    .add(contactData)
                    .await()
                
                // Sauvegarde locale immédiate
                offlineEntryDao.insertNotificationContact(NotificationContactEntity(
                    id = docRef.id,
                    name = name,
                    email = email,
                    relationship = relationship,
                    addedAt = now.toDate().time
                ))
            } catch (e: Exception) {
                android.util.Log.e("NotifContactsVM", "Erreur ajout contact", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteContact(contactId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Supprimer sur Firestore
                db.collection("users").document(userId)
                    .collection("notificationContacts")
                    .document(contactId)
                    .delete()
                    .await()
                
                // Supprimer localement
                offlineEntryDao.deleteNotificationContact(contactId)
            } catch (e: Exception) {
                android.util.Log.e("NotifContactsVM", "Erreur suppression contact", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
