package com.example.phoenx.data.sync

import com.example.phoenx.data.local.RankingEntity
import com.google.firebase.firestore.DocumentSnapshot

fun RankingEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "title" to title,
        "itemCount" to itemCount,
        "items" to items,
        "coverImageUrl" to coverImageUrl,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toRankingEntity(): RankingEntity {
    return RankingEntity(
        id = getString("id") ?: id,
        title = getString("title") ?: "",
        itemCount = getLong("itemCount")?.toInt() ?: 5,
        items = getString("items") ?: "",
        coverImageUrl = getString("coverImageUrl"),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
        syncStatus = "synced"
    )
}
