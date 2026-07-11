package com.example.phoenx.data.sync

import com.example.phoenx.data.local.OfflineEntry
import com.google.firebase.firestore.Blob
import org.json.JSONObject

/**
 * Extension pour convertir une OfflineEntry (Room) en Map pour Firestore.
 */
fun OfflineEntry.toFirestoreMap(): Map<String, Any?> {
    val ageMap = try {
        val json = JSONObject(ageAtCreation)
        mapOf(
            "years" to json.optInt("years"),
            "months" to json.optInt("months"),
            "days" to json.optInt("days")
        )
    } catch (e: Exception) {
        android.util.Log.w("EntryMapper", "Erreur parsing ageAtCreation pour $id: ${e.message}")
        null
    }

    return mapOf(
        "encryptedContent" to Blob.fromBytes(encryptedPayload),
        "type" to entryType,
        "ageAtCreation" to ageMap,
        "emotionalCategory" to emotionalCategory,
        "visibility" to visibility,
        "recipientIds" to recipientIds.split(",").filter { it.isNotBlank() },
        "compartmentIds" to compartmentIds.trim(',').split(",").filter { it.isNotBlank() },
        "isYoungSelfLetter" to isYoungSelfLetter,
        "targetAge" to targetAge,
        "createdAt" to createdAt,
        "aiSummary" to aiSummary,
        "aiTags" to aiTags,
        "enigmaQuestion" to enigmaQuestion,
        "enigmaAnswer" to enigmaAnswer,
        "scheduledTimestamp" to scheduledTimestamp,
        "unlockAfterDays" to unlockAfterDays,
        "unlockedAt" to unlockedAt,
        "fallbackAnswer" to fallbackAnswer,
        "latitude" to latitude,
        "longitude" to longitude,
        "locationName" to locationName,
        "pactId" to pactId,
        "locationId" to locationId,
        "mediaUrl" to mediaUrl,
        "memoryDate" to memoryDate,
        "memoryDateStart" to memoryDateStart,
        "memoryDateEnd" to memoryDateEnd
    )
}
