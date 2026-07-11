package com.example.phoenx.ui.screens.depositary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class RedeemState {
    object Loading : RedeemState()
    object Success : RedeemState()
    data class Error(val message: String) : RedeemState()
}

@HiltViewModel
class DepositaryViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositaryUiState())
    val uiState: StateFlow<DepositaryUiState> = _uiState.asStateFlow()

    private val _activationSuccess = MutableSharedFlow<Boolean>()
    val activationSuccess: SharedFlow<Boolean> = _activationSuccess.asSharedFlow()

    private val _joinSuccess = MutableSharedFlow<Boolean>()
    val joinSuccess: SharedFlow<Boolean> = _joinSuccess.asSharedFlow()

    private val _redeemState = MutableStateFlow<RedeemState>(RedeemState.Loading)
    val redeemState: StateFlow<RedeemState> = _redeemState.asStateFlow()

    // Persistance des données de liaison via SavedStateHandle (survit à la navigation)
    private var pendingJoinData: Triple<String, String, String>?
        get() = savedStateHandle.get<List<String>>("pending_join_data")?.let { 
            Triple(it[0], it[1], it[2]) 
        }
        set(value) = savedStateHandle.set("pending_join_data", value?.let { 
            listOf(it.first, it.second, it.third) 
        })

    fun redeemShortCode(shortCode: String) {
        // Garde : si on a déjà les données (retour de login), on ne rappelle pas la Cloud Function
        if (pendingJoinData != null) {
            _redeemState.value = RedeemState.Success
            return
        }

        _redeemState.value = RedeemState.Loading
        viewModelScope.launch {
            try {
                // 1. Échange du code court contre le token (SANS AUTH requise sur cette CF)
                val result = functions.getHttpsCallable("redeemDepositaryShortCode")
                    .call(mapOf("shortCode" to shortCode))
                    .await()
                
                val data = result.data as Map<*, *>
                val creatorId = data["creatorId"] as String
                val depositaryId = data["depositaryId"] as String
                val token = data["token"] as String
                
                // Mémoriser de façon persistante pour la liaison finale après auth
                pendingJoinData = Triple(creatorId, depositaryId, token)
                
                _redeemState.value = RedeemState.Success
                
            } catch (e: Exception) {
                val message = if (e is FirebaseFunctionsException) {
                    when (e.code) {
                        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                            "Ce lien d'invitation a expiré. Demande un nouveau lien à ton proche."
                        FirebaseFunctionsException.Code.ALREADY_EXISTS ->
                            "Ce lien a déjà été utilisé. Si c'est toi qui l'as ouvert précédemment, continue normalement."
                        FirebaseFunctionsException.Code.NOT_FOUND ->
                            "Ce lien est invalide. Vérifie le lien reçu par email."
                        else ->
                            "Une erreur est survenue. Réessaie dans quelques instants."
                    }
                } else {
                    "Une erreur de connexion est survenue. Vérifie ton accès internet."
                }
                _redeemState.value = RedeemState.Error(message)
                android.util.Log.e("PHOENX_AUTH", "Erreur rachat code: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Liaison effective entre le Dépositaire (connecté) et le Créateur.
     * Nécessite request.auth côté serveur.
     */
    fun confirmJoin(onSuccess: () -> Unit) {
        val data = pendingJoinData ?: return
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val joinData = hashMapOf(
                    "creatorId" to data.first,
                    "depositaryId" to data.second,
                    "token" to data.third
                )
                functions.getHttpsCallable("joinAsDepositary")
                    .call(joinData)
                    .await()
                
                _joinSuccess.emit(true)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_AUTH", "Erreur liaison finale: ${e.message}")
            }
        }
    }

    // Gardé pour compatibilité ou appel direct si besoin
    fun joinAsDepositary(creatorId: String, depositaryId: String, token: String) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "token" to token
                )
                functions.getHttpsCallable("joinAsDepositary")
                    .call(data)
                    .await()
                
                _joinSuccess.emit(true)
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_AUTH", "Erreur liaison Dépositaire: ${e.message}")
            }
        }
    }

    fun loadCreatorStatus(creatorId: String) {
        val myUid = auth.currentUser?.uid ?: return
        android.util.Log.d("PHOENX_DEBUG", "Entrée dans loadCreatorStatus avec ID=$creatorId")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. CHARGEMENT PRIORITAIRE DU CRÉATEUR (Pour débloquer l'UI immédiatement)
                val doc = db.collection("users").document(creatorId).get().await()
                val name = doc.getString("displayName") ?: "Proche"
                val missedCycles = doc.get("silenceConfig.missedCycles")?.toString()?.toInt() ?: 0
                val threshold = doc.get("silenceConfig.thresholdHours")?.toString()?.toInt() ?: 72
                val lastCheckInAt = doc.getTimestamp("silenceConfig.lastCheckInAt")
                
                val daysSince = if (lastCheckInAt != null) {
                    (System.currentTimeMillis() - lastCheckInAt.toDate().time) / (1000 * 60 * 60 * 24)
                } else 0

                // Mise à jour immédiate des données vitales
                _uiState.update { it.copy(
                    creatorName = name,
                    missedCycles = missedCycles,
                    daysSinceLastCheckIn = daysSince.toInt(),
                    thresholdHours = threshold
                ) }
                android.util.Log.d("PHOENX_DEBUG", "UI débloquée pour $name")

                // 2. REQUÊTES SECONDAIRES (Isolées pour ne pas faire planter l'UI en cas d'erreur)
                
                // Notifications de base
                val dynamicNotifications = mutableListOf<PhoenXNotification>()
                dynamicNotifications.add(PhoenXNotification(
                    id = "role_info",
                    message = "Rappel : Tu es le Dépositaire de $name.",
                    timestamp = System.currentTimeMillis(),
                    type = "info"
                ))

                if (missedCycles > 0) {
                    dynamicNotifications.add(PhoenXNotification(
                        id = "silence_alert",
                        message = "Alerte de silence : $name n'a pas répondu depuis $daysSince jours.",
                        timestamp = lastCheckInAt?.toDate()?.time ?: System.currentTimeMillis(),
                        type = "alert"
                    ))
                }

                // Vérification du protocole (Corrigée avec filter depositaryId)
                try {
                    val protocolSnap = db.collection("activationProtocols")
                        .whereEqualTo("creatorId", creatorId)
                        .whereEqualTo("depositaryId", myUid) // Obligatoire pour les règles Firestore
                        .whereEqualTo("status", "pending_contest")
                        .get().await()
                    
                    if (!protocolSnap.isEmpty) {
                        dynamicNotifications.add(0, PhoenXNotification(
                            id = "protocol_active",
                            message = "Activation confirmée. Délai de ${threshold}h en cours.",
                            timestamp = System.currentTimeMillis(),
                            type = "alert"
                        ))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PHOENX_DEBUG", "Erreur check protocole: ${e.message}")
                }

                // Profil personnel
                var myName = ""
                val myEmail = auth.currentUser?.email ?: ""
                try {
                    val myDoc = db.collection("users").document(myUid).get().await()
                    myName = myDoc.getString("displayName") ?: myEmail.substringBefore("@")
                } catch (e: Exception) {
                    android.util.Log.e("PHOENX_DEBUG", "Erreur profil perso: ${e.message}")
                }

                // Mise à jour finale
                _uiState.update { it.copy(
                    personalName = myName,
                    personalEmail = myEmail,
                    notifications = dynamicNotifications,
                    isLoading = false
                ) }

            } catch (e: Exception) {
                android.util.Log.e("DepositaryVM", "Error loading status", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resolveAlert(creatorId: String, depositaryId: String, note: String?) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "note" to note
                )
                functions.getHttpsCallable("resolveCreatorSilence")
                    .call(data)
                    .await()

                loadCreatorStatus(creatorId)
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_ALERT", "Erreur résolution alerte: ${e.message}")
            }
        }
    }

    fun activateProtocol(
        creatorId: String,
        depositaryId: String,
        contactAttemptNote: String,
        contactAttemptDetails: Map<String, Boolean>,
        depositaryNote: String?
    ) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "contactAttemptNote" to contactAttemptNote,
                    "contactAttemptDetails" to contactAttemptDetails,
                    "depositaryNote" to depositaryNote
                )
                functions.getHttpsCallable("activateProtocol")
                    .call(data)
                    .await()
                
                _activationSuccess.emit(true)
            } catch (e: Exception) {
                android.util.Log.e("DepositaryVM", "Error activating protocol", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class PhoenXNotification(
    val id: String,
    val message: String,
    val timestamp: Long,
    val type: String // "alert" | "info" | "success"
)

data class DepositaryUiState(
    val creatorName: String = "",
    val missedCycles: Int = 0,
    val daysSinceLastCheckIn: Int = 0,
    val thresholdHours: Int = 72,
    val isLoading: Boolean = true,
    val personalName: String = "",
    val personalEmail: String = "",
    val notifications: List<PhoenXNotification> = emptyList()
)
