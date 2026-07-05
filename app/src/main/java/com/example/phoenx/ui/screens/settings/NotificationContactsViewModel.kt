package com.example.phoenx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class NotificationContact(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val relationship: String = "",
    val addedAt: Long = 0L
)

@HiltViewModel
class NotificationContactsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<NotificationContact>>(emptyList())
    val contacts: StateFlow<List<NotificationContact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot: QuerySnapshot = db.collection("users").document(userId)
                    .collection("notificationContacts")
                    .orderBy("addedAt")
                    .get()
                    .await()
                
                val list = snapshot.documents.map { doc ->
                    NotificationContact(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: 0L
                    )
                }
                _contacts.value = list
            } catch (e: Exception) {
                android.util.Log.e("NotifContactsVM", "Erreur chargement contacts", e)
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
                val contactData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "relationship" to relationship,
                    "addedAt" to com.google.firebase.Timestamp.now()
                )
                db.collection("users").document(userId)
                    .collection("notificationContacts")
                    .add(contactData)
                    .await()
                
                loadContacts()
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
                db.collection("users").document(userId)
                    .collection("notificationContacts")
                    .document(contactId)
                    .delete()
                    .await()
                
                loadContacts()
            } catch (e: Exception) {
                android.util.Log.e("NotifContactsVM", "Erreur suppression contact", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
