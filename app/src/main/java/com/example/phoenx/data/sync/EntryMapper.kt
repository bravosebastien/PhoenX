package com.example.phoenx.data.sync

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import org.json.JSONObject

/**
 * Extension pour convertir une OfflineEntry (Room) en Map pour Firestore.
 */
fun OfflineEntry.toFirestoreMap(encryptionManager: EncryptionManager): Map<String, Any?> {
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
        "recipientIds" to recipientIds.split(",").filter { it.isNotBlank() }.map { it.trim() },
        "compartmentIds" to compartmentIds.trim(',').split(",").filter { it.isNotBlank() }.map { it.trim() },
        "isYoungSelfLetter" to isYoungSelfLetter,
        "targetAge" to targetAge,
        "createdAt" to createdAt,
        // CHIFFREMENT v9.4.12 : Conversion String -> Blob (AES-256-GCM)
        "aiSummary" to if (aiSummary.isNotEmpty()) Blob.fromBytes(encryptionManager.encryptText(aiSummary)) else "",
        "aiTags" to if (aiTags.isNotEmpty()) Blob.fromBytes(encryptionManager.encryptText(aiTags)) else "",
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
        "personIds" to personIds.split(",").filter { it.isNotBlank() }.map { it.trim() },
        "isUltimateSecret" to isUltimateSecret,
        "silentAttribution" to silentAttribution,
        "includeInBook" to includeInBook,
        "soulTone" to soulTone
    )
}

fun com.example.phoenx.data.local.PersonEntity.toFirestoreMap(storageUrl: String? = null): Map<String, Any?> {
    return mapOf(
        "prenom" to firstName,
        "nom" to lastName,
        "lien" to relationship,
        "distinctionType" to distinctionType,
        "distinctionValeur" to distinctionValue,
        "imageUrl" to storageUrl, // v8.9.9 : URL Storage pour synchronisation
        "createdAt" to createdAt
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en PersonEntity (Room).
 */
fun DocumentSnapshot.toPersonEntity(): com.example.phoenx.data.local.PersonEntity {
    return com.example.phoenx.data.local.PersonEntity(
        id = id,
        firstName = getString("prenom") ?: "",
        lastName = getString("nom"),
        relationship = getString("lien"),
        distinctionType = getString("distinctionType"),
        distinctionValue = getString("distinctionValeur"),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        syncStatus = "synced"
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en OfflineEntry (Room).
 * (v8.5.5 - Support Heritage sans Sync local)
 */
fun DocumentSnapshot.toOfflineEntry(encryptionManager: EncryptionManager, explicitKey: ByteArray? = null): OfflineEntry? {
    if (!exists()) return null
    val ageMap = get("ageAtCreation") as? Map<*, *>
    val ageJson = ageMap?.let { JSONObject(it).toString() } ?: "{}"

    val recIds = (get("recipientIds") as? List<*>)?.joinToString(",") ?: ""
    val compIds = (get("compartmentIds") as? List<*>)?.let { "," + it.joinToString(",") + "," } ?: ""

    // DÉTECTION & DÉCHIFFREMENT HYBRIDE (v9.4.12)
    val summaryObj = get("aiSummary")
    val finalSummary = when (summaryObj) {
        is Blob -> encryptionManager.decryptText(summaryObj.toBytes(), explicitKey)
        is String -> summaryObj
        else -> ""
    }

    val tagsObj = get("aiTags")
    val finalTags = when (tagsObj) {
        is Blob -> encryptionManager.decryptText(tagsObj.toBytes(), explicitKey)
        is List<*> -> tagsObj.joinToString(",")
        is String -> tagsObj
        else -> ""
    }

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
        aiSummary = finalSummary,
        aiTags = finalTags,
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
        silentAttribution = getBoolean("silentAttribution") ?: false,
        includeInBook = getBoolean("includeInBook") ?: true,
        soulTone = getString("soulTone")
    )
}
