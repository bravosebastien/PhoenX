package com.example.phoenx.data.sync

import com.example.phoenx.data.local.RecipientEntity
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Extension pour convertir un RecipientEntity (Room) en Map pour Firestore.
 */
fun RecipientEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "email" to email,
        "relationship" to relationship,
        "accessLevel" to accessLevel,
        "canAskQuestions" to canAskQuestions,
        "maxQuestionsAllowed" to maxQuestionsAllowed,
        "questionsAskedCount" to questionsAskedCount,
        "accessToken" to accessToken,
        "invitationSentAt" to invitationSentAt,
        "invitationConfirmed" to invitationConfirmed,
        "photoUrl" to photoUrl,
        "createdAt" to createdAt,
        "phone" to phone,
        "linkedUid" to linkedUid
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en RecipientEntity (Room).
 */
fun DocumentSnapshot.toRecipientEntity(): RecipientEntity {
    return RecipientEntity(
        id = id,
        name = getString("name") ?: "Anonyme",
        email = getString("email") ?: "",
        relationship = getString("relationship") ?: "Proche",
        accessLevel = getString("accessLevel") ?: "full",
        canAskQuestions = getBoolean("canAskQuestions") ?: false,
        maxQuestionsAllowed = getLong("maxQuestionsAllowed")?.toInt(),
        questionsAskedCount = getLong("questionsAskedCount")?.toInt() ?: 0,
        accessToken = getString("accessToken"),
        invitationSentAt = getLong("invitationSentAt"),
        invitationConfirmed = getBoolean("invitationConfirmed") ?: false,
        photoUrl = getString("photoUrl"),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        phone = getString("phone"),
        linkedUid = getString("linkedUid")
    )
}
