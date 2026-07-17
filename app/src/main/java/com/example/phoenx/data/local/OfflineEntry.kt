package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "offline_entries")
data class OfflineEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val creatorUid: String = "",
    val encryptedPayload: ByteArray,
    val entryType: String,
    val ageAtCreation: String, // Stocké en JSON
    val emotionalCategory: String,
    val visibility: String,
    val recipientIds: String = "",
    val isYoungSelfLetter: Boolean = false,
    val targetAge: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending", // "pending" | "synced" | "failed"
    
    // CHAMPS IA LOCALE (Signature 5.0)
    val aiSummary: String = "",
    val aiTags: String = "",

    // MODE DÉTECTIVE & BOÎTE AUX LETTRES (ADN 5.0)
    val enigmaQuestion: String? = null,
    val enigmaAnswer: String? = null,
    val scheduledTimestamp: Long? = null,
    val unlockAfterDays: Int = 30,
    val unlockedAt: Long? = null,
    val fallbackAnswer: String? = null, // Chiffré Tink

    // GÉOLOCALISATION (Signature 5.0 - La Mappemonde)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,

    // LE PACTE (Signature 5.0)
    val pactId: String? = null,

    // GÉOLOCALISATION AVANCÉE (Signature 7.1)
    val locationId: String? = null,

    // RANGEMENT MULTIPLE (Signature 7.2)
    val compartmentIds: String = "", // Stocké en CSV avec virgules encadrantes : ,ID1,ID2,

    // PIPELINE MÉDIA (Signature 7.3 - Réservé v13)
    val mediaUrl: String? = null,
    val localMediaPath: String? = null,

    // ÉDITION AVANCÉE (Signature 7.4 - v14)
    val memoryDate: Long? = null, // Date réelle du souvenir (distincte de createdAt)

    // PÉRIODE (Signature 7.5 - v15)
    val memoryDateStart: Long? = null,
    val memoryDateEnd: Long? = null,

    // COMPLÉMENTS MULTI-MÉDIA (Signature 7.6 - v18)
    val parentEntryId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OfflineEntry

        if (id != other.id) return false
        if (creatorUid != other.creatorUid) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (entryType != other.entryType) return false
        if (ageAtCreation != other.ageAtCreation) return false
        if (emotionalCategory != other.emotionalCategory) return false
        if (visibility != other.visibility) return false
        if (recipientIds != other.recipientIds) return false
        if (isYoungSelfLetter != other.isYoungSelfLetter) return false
        if (targetAge != other.targetAge) return false
        if (createdAt != other.createdAt) return false
        if (syncStatus != other.syncStatus) return false
        if (aiSummary != other.aiSummary) return false
        if (aiTags != other.aiTags) return false
        if (enigmaQuestion != other.enigmaQuestion) return false
        if (enigmaAnswer != other.enigmaAnswer) return false
        if (scheduledTimestamp != other.scheduledTimestamp) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (locationName != other.locationName) return false
        if (locationId != other.locationId) return false
        if (compartmentIds != other.compartmentIds) return false
        if (mediaUrl != other.mediaUrl) return false
        if (localMediaPath != other.localMediaPath) return false
        if (memoryDate != other.memoryDate) return false
        if (memoryDateStart != other.memoryDateStart) return false
        if (memoryDateEnd != other.memoryDateEnd) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + creatorUid.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + entryType.hashCode()
        result = 31 * result + ageAtCreation.hashCode()
        result = 31 * result + emotionalCategory.hashCode()
        result = 31 * result + visibility.hashCode()
        result = 31 * result + recipientIds.hashCode()
        result = 31 * result + isYoungSelfLetter.hashCode()
        result = 31 * result + (targetAge ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + syncStatus.hashCode()
        result = 31 * result + aiSummary.hashCode()
        result = 31 * result + aiTags.hashCode()
        result = 31 * result + (enigmaQuestion?.hashCode() ?: 0)
        result = 31 * result + (enigmaAnswer?.hashCode() ?: 0)
        result = 31 * result + (scheduledTimestamp?.hashCode() ?: 0)
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (locationName?.hashCode() ?: 0)
        result = 31 * result + (locationId?.hashCode() ?: 0)
        result = 31 * result + compartmentIds.hashCode()
        result = 31 * result + (mediaUrl?.hashCode() ?: 0)
        result = 31 * result + (localMediaPath?.hashCode() ?: 0)
        result = 31 * result + (memoryDate?.hashCode() ?: 0)
        result = 31 * result + (memoryDateStart?.hashCode() ?: 0)
        result = 31 * result + (memoryDateEnd?.hashCode() ?: 0)
        return result
    }
}
