package com.example.phoenx.data.sync

import com.example.phoenx.data.local.OfflineEntry
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
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
        "uid" to creatorUid,
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
        "memoryDateEnd" to memoryDateEnd,
        "parentEntryId" to parentEntryId,
        "enigmaHint" to enigmaHint,
        "enigmaAutoUnlockDays" to enigmaAutoUnlockDays,
        "questionId" to questionId,
        "personIds" to personIds.split(",").filter { it.isNotBlank() },
        "isUltimateSecret" to isUltimateSecret,
        "silentAttribution" to silentAttribution
    )
}

fun com.example.phoenx.data.local.PersonEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "prenom" to firstName,
        "nom" to lastName,
        "lien" to relationship,
        "distinctionType" to distinctionType,
        "distinctionValeur" to distinctionValue,
        "createdAt" to createdAt
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en OfflineEntry (Room).
 * (v8.5.5 - Support Heritage sans Sync local)
 */
fun DocumentSnapshot.toOfflineEntry(): OfflineEntry? {
    if (!exists()) return null
    val ageMap = get("ageAtCreation") as? Map<*, *>
    val ageJson = ageMap?.let { JSONObject(it).toString() } ?: "{}"

    val recIds = (get("recipientIds") as? List<*>)?.joinToString(",") ?: ""
    val compIds = (get("compartmentIds") as? List<*>)?.let { "," + it.joinToString(",") + "," } ?: ""

    return OfflineEntry(
        id = id,
        creatorUid = getString("uid") ?: "",
        encryptedPayload = (get("encryptedContent") as? Blob)?.toBytes() ?: ByteArray(0),
        entryType = getString("type") ?: "TEXT",
        ageAtCreation = ageJson,
        emotionalCategory = getString("emotionalCategory") ?: "",
        visibility = getString("visibility") ?: "RESTRICTED",
        recipientIds = recIds,
        compartmentIds = compIds,
        isYoungSelfLetter = getBoolean("isYoungSelfLetter") ?: false,
        targetAge = getLong("targetAge")?.toInt(),
        createdAt = getLong("createdAt") ?: 0L,
        aiSummary = getString("aiSummary") ?: "",
        aiTags = (get("aiTags") as? List<*>)?.joinToString(",") ?: "",
        enigmaQuestion = getString("enigmaQuestion"),
        enigmaAnswer = getString("enigmaAnswer"),
        fallbackAnswer = getString("fallbackAnswer"),
        mediaUrl = getString("mediaUrl"),
        localMediaPath = null,
        memoryDate = getLong("memoryDate"),
        memoryDateStart = getLong("memoryDateStart"),
        memoryDateEnd = getLong("memoryDateEnd"),
        parentEntryId = getString("parentEntryId"),
        enigmaHint = getString("enigmaHint"),
        enigmaAutoUnlockDays = getLong("enigmaAutoUnlockDays")?.toInt(),
        questionId = getString("questionId"),
        personIds = (get("personIds") as? List<*>)?.joinToString(",") ?: "",
        isUltimateSecret = getBoolean("isUltimateSecret") ?: false,
        silentAttribution = getBoolean("silentAttribution") ?: false
    )
}
