package com.example.phoenx.data.heir

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.sync.toOfflineEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Résultat du chargement d'un souvenir en mode Destinataire (Héritier).
 */
data class HeirLoadResult(
    val isActivated: Boolean,
    val heirKey: ByteArray?,
    val firestoreEntry: OfflineEntry?,
    val firestoreComplements: List<OfflineEntry>
)

/**
 * Charge un souvenir et ses compléments depuis Firestore, pour le cas où
 * un Destinataire (Héritier) consulte le souvenir d'un Créateur.
 * Extrait de MemoryDetailViewModel.loadEntry — étape 2/7 du découpage.
 */
@Singleton
class HeirEntryLoader @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: EncryptionManager
) {
    suspend fun load(creatorId: String, entryId: String): HeirLoadResult {
        val result = functions.getHttpsCallable("getCreatorProtocolStatus")
            .call(mapOf("creatorId" to creatorId)).await()

        val data = result.data as? Map<*, *>
        val isActivated = data?.get("isActivated") as? Boolean ?: false
        android.util.Log.d("PHOENX_MEMORY_OPEN_TRACE", "Protocole check: isActivated=$isActivated")

        var heirKey: ByteArray? = null
        if (isActivated) {
            val keyDoc = db.collection("users").document(creatorId)
                .collection("entry_keys").document("main").get().await()
            val keyBase64 = keyDoc.getString("key")
            if (keyBase64 != null) {
                heirKey = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
            }
        }

        val entryDoc = db.collection("users").document(creatorId)
            .collection("entries").document(entryId).get().await()

        var firestoreEntry: OfflineEntry? = null
        var firestoreComplements: List<OfflineEntry> = emptyList()

        if (entryDoc.exists()) {
            firestoreEntry = entryDoc.toOfflineEntry(encryptionManager, heirKey)

            try {
                val compResult = functions.getHttpsCallable("getEntryComplements")
                    .call(mapOf("creatorId" to creatorId, "entryId" to entryId)).await()

                val compData = compResult.data as? Map<*, *>
                val compsList = compData?.get("complements") as? List<Map<String, Any?>>
                android.util.Log.d("PHOENX_MEMORY_OPEN_TRACE", "Cloud Function Result: count=${compsList?.size}, heirKeyPresent=${heirKey != null}")

                firestoreComplements = compsList?.mapNotNull { map ->
                    val mapped = map.toOfflineEntry(encryptionManager, heirKey)
                    android.util.Log.d("PHOENX_MEMORY_OPEN_TRACE", "Mapped Complement: id=${mapped?.id}, aiSummary=${mapped?.aiSummary}, mediaUrl=${mapped?.mediaUrl}")
                    mapped
                } ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_MEMORY_OPEN_TRACE", "ERREUR Cloud Function getEntryComplements id=$entryId: ${e.message}", e)
            }
        }

        return HeirLoadResult(isActivated, heirKey, firestoreEntry, firestoreComplements)
    }
}