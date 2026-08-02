package com.example.phoenx.ui.screens.genealogy

import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.domain.model.ResolvedPerson

fun PersonEntity.toResolvedPerson(photoUrl: String?): ResolvedPerson {
    return ResolvedPerson(
        id = id,
        firstName = firstName,
        lastName = lastName,
        photoUrl = photoUrl,
        isDeceased = isDeceased,
        biography = biography,
        parentIds = parentIds.trim(',').split(",").filter { it.isNotBlank() }
    )
}
