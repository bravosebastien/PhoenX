package com.example.phoenx.ui.screens.encounters

import com.example.phoenx.data.local.PersonEntity
import kotlin.math.abs
import kotlin.math.sin

/**
 * EncounterGraphAlgorithm (v9.5.3 - ÉTAPE B2)
 * Positionnement avec Chemin Central et Branches.
 */

data class EncounterNode(
    val person: PersonEntity,
    val x: Float, // en dp, relatif au centre
    val y: Float, // en dp, relatif au minAge
    val isFamily: Boolean = false,
    val parentId: String? = null // introducedById
)

data class EncounterLayout(
    val nodes: List<EncounterNode>,
    val totalHeightDp: Float
)

object EncounterGraphAlgorithm {
    private const val DP_PER_YEAR = 60f 
    private const val MARGIN_TOP_DP = 60f
    private const val NODE_X_SPACING_DP = 180f
    private const val FAMILY_Y_OFFSET = -150f

    fun calculateLayout(
        encounters: List<PersonEntity>,
        allPersons: List<PersonEntity>
    ): EncounterLayout {
        if (encounters.isEmpty()) return EncounterLayout(emptyList(), 0f)

        val minAge = encounters.mapNotNull { it.encounterAge }.minOrNull() ?: 0
        val nodes = mutableListOf<EncounterNode>()
        
        // 1. Identifier les présentateurs FAMILY (ceux qui ne sont pas dans encounters mais cités)
        val encounterIds = encounters.map { it.id }.toSet()
        val familyIntroducerIds = encounters.mapNotNull { it.introducedById }
            .filter { it !in encounterIds }
            .toSet()
        
        val familyIntroducers = allPersons.filter { it.id in familyIntroducerIds }

        // 2. Créer les noeuds FAMILY (Passerelles)
        familyIntroducers.forEach { familyPerson ->
            // Hauteur = âge de la PREMIÈRE rencontre qu'il a permise - 150dp
            val firstEncounterAge = encounters
                .filter { it.introducedById == familyPerson.id }
                .mapNotNull { it.encounterAge }
                .minOrNull() ?: minAge
            
            val y = (firstEncounterAge - minAge) * DP_PER_YEAR + MARGIN_TOP_DP + FAMILY_Y_OFFSET
            nodes.add(EncounterNode(
                person = familyPerson,
                x = getPathX(y), // Positionnés sur le chemin central initialement
                y = y,
                isFamily = true
            ))
        }

        // 3. Créer les noeuds RENCONTRES
        encounters.forEach { person ->
            val age = person.encounterAge ?: minAge
            val y = (age - minAge) * DP_PER_YEAR + MARGIN_TOP_DP
            nodes.add(EncounterNode(
                person = person,
                x = getPathX(y),
                y = y,
                isFamily = false,
                parentId = person.introducedById
            ))
        }

        // 4. Ajustement horizontal (évitement de collisions et branches)
        // On traite par Y croissant
        val finalNodes = mutableListOf<EncounterNode>()
        val sortedNodes = nodes.sortedBy { it.y }

        sortedNodes.forEach { node ->
            var finalX = node.x
            
            if (node.parentId != null) {
                // Si présenté par quelqu'un, on essaie de se décaler par rapport au présentateur
                val parent = finalNodes.find { it.person.id == node.parentId }
                if (parent != null) {
                    // On se place à droite du parent par défaut, ou on alterne
                    val side = if (node.person.id.hashCode() % 2 == 0) 1f else -1f
                    finalX = parent.x + (side * NODE_X_SPACING_DP)
                }
            }

            // Évitement de collision avec les noeuds déjà placés au même Y (ou très proche)
            while (finalNodes.any { abs(it.y - node.y) < 10f && abs(it.x - finalX) < 100f }) {
                finalX += 160f // Décalage vers la droite tant qu'il y a collision
            }
            
            finalNodes.add(node.copy(x = finalX))
        }

        val maxNodeY = finalNodes.maxOfOrNull { it.y } ?: 0f
        val totalHeight = maxNodeY + 200f

        // Logs PHOENX_GRAPH augmentés
        android.util.Log.e("PHOENX_GRAPH", "Calcul Layout B2. Nodes: ${finalNodes.size}, Height: $totalHeight dp")
        finalNodes.forEach { node ->
            val attachment = if (node.parentId == null) "Chemin Central" else "Branche (${node.parentId})"
            android.util.Log.e("PHOENX_GRAPH", "Node: ${node.person.firstName}, Family: ${node.isFamily}, X=${node.x}, Y=${node.y}, Attachment: $attachment")
        }

        return EncounterLayout(finalNodes, totalHeight)
    }

    /**
     * Calcule la position X du chemin sinueux à un Y donné.
     */
    fun getPathX(y: Float): Float {
        return sin(y / 800f) * 100f
    }
}
