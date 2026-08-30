package com.example.phoenx.ui.screens.genealogy

import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.domain.model.ResolvedPerson

fun PersonEntity.toResolvedPerson(resolvedUrl: String?): ResolvedPerson {
    val rawPath = encounterImagePath ?: imagePath
    val isLocal = rawPath?.let { it.startsWith("/") || it.startsWith("file://") } == true
    
    // Nettoyage du préfixe file:// pour localPath si nécessaire
    val cleanLocalPath = if (isLocal && rawPath != null) {
        if (rawPath.startsWith("file://")) rawPath.substring(7) else rawPath
    } else null

    return ResolvedPerson(
        id = id,
        firstName = firstName,
        lastName = lastName,
        // photoUrl : l'URL résolue du cache, ou à défaut le chemin Storage (users/...)
        photoUrl = resolvedUrl ?: (if (!isLocal) rawPath else null),
        isDeceased = isDeceased,
        biography = biography,
        parentIds = parentIds.trim(',').split(",").filter { it.isNotBlank() },
        isReparented = isReparented,
        reparentedRelationLabel = reparentedRelationLabel,
        photoField = if (encounterImagePath != null) "encounterImagePath" else "imageUrl",
        localPath = cleanLocalPath
    )
}
