package com.example.phoenx.data.sync

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import org.json.JSONObject

/**
 * PHOEN-X v9.4.27 - Support de la lecture via Cloud Functions (Map) en plus du SDK Firestore (DocumentSnapshot)
 */
private fun Any?.extractBytes(): ByteArray {
    val res = when (this) {
        is Blob -> this.toBytes()
        is Map<*, *> -> {
            val b64 = this["_base64"] as? String
            if (b64 != null) {
                try {
                    android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    android.util.Log.e("PHOENX_MEMORY_OPEN_TRACE", "Base64 Decode Error: ${e.message}")
                    ByteArray(0)
                }
            } else {
                android.util.Log.w("PHOENX_MEMORY_OPEN_TRACE", "Map keys found: ${this.keys}")
                ByteArray(0)
            }
        }
        else -> {
            if (this != null) android.util.Log.w("PHOENX_MEMORY_OPEN_TRACE", "extractBytes unknown type: ${this.javaClass.simpleName}")
            ByteArray(0)
        }
    }
    return res
}

private fun Any?.asLong(): Long? {
    return when (this) {
        is Long -> this
        is Number -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }
}

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
        "soulTone" to soulTone,
        // ENRICHISSEMENT MÉDIA v9.4.27 (Migration v43)
        "userComment" to userComment,
        "coverUrl" to coverUrl,
        "mediaProvider" to mediaProvider
    )
}

fun com.example.phoenx.data.local.PersonEntity.toFirestoreMap(storageUrl: String? = null): Map<String, Any?> {
    return mapOf(
        "prenom" to firstName.trim(),
        "nom" to lastName?.trim(),
        "lien" to relationship?.trim(),
        "distinctionType" to distinctionType,
        "distinctionValeur" to distinctionValue?.trim(),
        "imageUrl" to (storageUrl ?: imagePath), // v8.9.9 : URL Storage pour synchronisation
        "createdAt" to createdAt,
        // v9.4.24: Genealogy fields
        "parentIds" to parentIds,
        "isDeceased" to isDeceased,
        "biography" to biography.trim(),
        "isReparented" to isReparented,
        "reparentedRelationLabel" to reparentedRelationLabel?.trim(),
        // LES RENCONTRES v9.5.0
        "categories" to categories.split(",").filter { it.isNotBlank() }.distinct().joinToString(",", prefix = ",", postfix = ","),
        "introducedById" to introducedById,
        "encounterAge" to encounterAge,
        "encounterLocationId" to encounterLocationId,
        "encounterLocationLabel" to encounterLocationLabel?.trim(),
        "linkNature" to linkNature?.trim(),
        "linkStatus" to linkStatus,
        "visibility" to visibility,
        // REFONTE GALERIE v9.6.0
        "encounterContext" to encounterContext,
        "encounterContextLabel" to encounterContextLabel?.trim(),
        "relationEndAge" to relationEndAge,
        "relationEndReason" to relationEndReason?.trim()
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en PersonEntity (Room).
 */
fun DocumentSnapshot.toPersonEntity(): com.example.phoenx.data.local.PersonEntity {
    val rawFirstName = getString("prenom") ?: ""
    val rawLastName = getString("nom")
    val rawBio = getString("biography") ?: ""
    val rawParentIds = getString("parentIds") ?: ""
    val isDeceasedVal = getBoolean("isDeceased") ?: false
    val isReparentedVal = getBoolean("isReparented") ?: false
    val rawReparentedLabel = getString("reparentedRelationLabel")
    
    // ADN 5.0 : On lit la valeur brute sans tenter de recalculer ou de deviner
    val categoriesVal = getString("categories") ?: ""

    return com.example.phoenx.data.local.PersonEntity(
        id = id,
        firstName = rawFirstName.trim(),
        lastName = rawLastName?.trim(),
        relationship = (getString("linkNature") ?: getString("lien"))?.trim(),
        distinctionType = getString("distinctionType"),
        distinctionValue = getString("distinctionValeur")?.trim(),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        syncStatus = "synced",
        // v9.4.24: Genealogy fields
        parentIds = rawParentIds,
        isDeceased = isDeceasedVal,
        biography = rawBio.trim(),
        isReparented = isReparentedVal,
        reparentedRelationLabel = rawReparentedLabel?.trim(),
        imagePath = getString("imageUrl"),
        // LES RENCONTRES v9.5.0
        categories = categoriesVal.split(",").filter { it.isNotBlank() }.distinct().joinToString(",", prefix = ",", postfix = ","),
        introducedById = getString("introducedById"),
        encounterAge = getLong("encounterAge")?.toInt(),
        encounterLocationId = getString("encounterLocationId"),
        encounterLocationLabel = getString("encounterLocationLabel")?.trim(),
        linkNature = getString("linkNature")?.trim(),
        linkStatus = getString("linkStatus"),
        visibility = getString("visibility") ?: "PUBLIC",
        // REFONTE GALERIE v9.6.0
        encounterContext = getString("encounterContext"),
        encounterContextLabel = getString("encounterContextLabel")?.trim(),
        relationEndAge = getLong("relationEndAge")?.toInt(),
        relationEndReason = getString("relationEndReason")?.trim()
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en OfflineEntry (Room).
 * (v8.5.5 - Support Heritage sans Sync local)
 */
fun DocumentSnapshot.toOfflineEntry(encryptionManager: EncryptionManager, explicitKey: ByteArray? = null): OfflineEntry? {
    if (!exists()) return null
    return (data ?: emptyMap<String, Any?>()).plus("id" to id).toOfflineEntry(encryptionManager, explicitKey)
}

/**
 * PHOEN-X v9.4.27 - Version générique pour mapper un souvenir depuis un Map (Cloud Functions)
 */
fun Map<String, Any?>.toOfflineEntry(encryptionManager: EncryptionManager, explicitKey: ByteArray? = null): OfflineEntry? {
    val id = this["id"] as? String ?: return null
    val ageMap = this["ageAtCreation"] as? Map<*, *>
    val ageJson = ageMap?.let { JSONObject(it).toString() } ?: "{}"

    val recIds = (this["recipientIds"] as? List<*>)?.joinToString(",") ?: ""
    val compIds = (this["compartmentIds"] as? List<*>)?.let { "," + it.joinToString(",") + "," } ?: ""

    // DÉTECTION & DÉCHIFFREMENT HYBRIDE (v9.4.12)
    val summaryObj = this["aiSummary"]
    val finalSummary = when {
        summaryObj is String -> summaryObj
        summaryObj != null -> encryptionManager.decryptText(summaryObj.extractBytes(), explicitKey)
        else -> ""
    }

    val tagsObj = this["aiTags"]
    val finalTags = when {
        tagsObj is String -> tagsObj
        tagsObj is List<*> -> tagsObj.joinToString(",")
        tagsObj != null -> encryptionManager.decryptText(tagsObj.extractBytes(), explicitKey)
        else -> ""
    }

    return OfflineEntry(
        id = id,
        creatorUid = this["uid"] as? String ?: "",
        encryptedPayload = this["encryptedContent"].extractBytes(),
        entryType = this["type"] as? String ?: "TEXT",
        ageAtCreation = ageJson,
        emotionalCategory = this["emotionalCategory"] as? String ?: "",
        visibility = this["visibility"] as? String ?: "RESTRICTED",
        recipientIds = recIds,
        compartmentIds = compIds,
        isYoungSelfLetter = this["isYoungSelfLetter"] as? Boolean ?: false,
        targetAge = this["targetAge"].asLong()?.toInt(),
        createdAt = this["createdAt"].asLong() ?: 0L,
        aiSummary = finalSummary,
        aiTags = finalTags,
        enigmaQuestion = this["enigmaQuestion"] as? String,
        enigmaAnswer = this["enigmaAnswer"] as? String,
        fallbackAnswer = this["fallbackAnswer"] as? String,
        mediaUrl = this["mediaUrl"] as? String,
        localMediaPath = null,
        memoryDate = this["memoryDate"].asLong(),
        memoryDateStart = this["memoryDateStart"].asLong(),
        memoryDateEnd = this["memoryDateEnd"].asLong(),
        parentEntryId = this["parentEntryId"] as? String,
        enigmaHint = this["enigmaHint"] as? String,
        enigmaAutoUnlockDays = this["enigmaAutoUnlockDays"].asLong()?.toInt(),
        questionId = this["questionId"] as? String,
        personIds = (this["personIds"] as? List<*>)?.joinToString(",") ?: "",
        isUltimateSecret = this["isUltimateSecret"] as? Boolean ?: false,
        silentAttribution = this["silentAttribution"] as? Boolean ?: false,
        includeInBook = this["includeInBook"] as? Boolean ?: true,
        soulTone = this["soulTone"] as? String,
        // v9.4.27
        userComment = this["userComment"] as? String,
        coverUrl = this["coverUrl"] as? String,
        mediaProvider = this["mediaProvider"] as? String
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en StandaloneMediaEntity (Room).
 */
fun DocumentSnapshot.toStandaloneMediaEntity(): com.example.phoenx.data.local.StandaloneMediaEntity {
    val type = getString("type") ?: ""
    val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
    
    val contentStr = if (needsEncryption) {
        val blob = get("content") as? Blob
        blob?.toBytes()?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) } ?: ""
    } else {
        getString("content") ?: ""
    }

    val recIds = (get("recipientIds") as? List<*>)?.mapNotNull { it.toString() }?.joinToString(",") ?: ""

    return com.example.phoenx.data.local.StandaloneMediaEntity(
        id = id,
        creatorUid = getString("uid") ?: "",
        type = type,
        title = getString("title") ?: "",
        userComment = getString("userComment"),
        content = contentStr,
        recipientIds = recIds,
        visibility = getString("visibility") ?: "RESTRICTED",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        syncStatus = "synced",
        coverUrl = getString("coverUrl"),
        mediaProvider = getString("mediaProvider")
    )
}
