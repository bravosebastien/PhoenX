package com.example.phoenx.data.memory

import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.domain.model.SimplifiedPerson
import com.example.phoenx.domain.model.toSimplified
import com.example.phoenx.domain.model.toSimplifiedRecipient
import com.example.phoenx.domain.model.toSimplifiedWitness
import com.example.phoenx.domain.model.toSimplifiedDepositary
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recherche et création de personnes citées dans un souvenir.
 * Extrait de MemoryDetailViewModel — étape 5/7 du découpage.
 */
@Singleton
class MemoryPersonSelectionManager @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) {
    suspend fun searchAllPersons(query: String): List<SimplifiedPerson> {
        val persons = offlineEntryDao.getAllPersons().first().toSimplified()
        val recipientsList = offlineEntryDao.getAllRecipients().first().toSimplifiedRecipient()
        val witnesses = offlineEntryDao.getAllWitnesses().first().toSimplifiedWitness()
        val depositaries = offlineEntryDao.getAllDepositaries().first().toSimplifiedDepositary()

        val allSimplified = persons + recipientsList + witnesses + depositaries
        return allSimplified
            .filter { it.name.contains(query, ignoreCase = true) }
            .distinctBy { it.name.lowercase().trim() }
    }

    suspend fun createPerson(
        firstName: String,
        lastName: String?,
        relationship: String?,
        distinctionType: String?,
        distinctionValue: String?,
        imagePath: String?,
        characterType: String
    ): SimplifiedPerson {
        val newPerson = PersonEntity(
            firstName = firstName.trim(),
            lastName = lastName?.trim(),
            relationship = relationship,
            distinctionType = distinctionType,
            distinctionValue = distinctionValue,
            imagePath = imagePath,
            characterType = characterType
        )
        offlineEntryDao.insertPerson(newPerson)

        return SimplifiedPerson(
            id = newPerson.id,
            name = newPerson.firstName + (newPerson.lastName?.let { " $it" } ?: ""),
            photoUrl = newPerson.imagePath,
            sourceType = "arbre_livre",
            relationship = newPerson.relationship
        )
    }
}