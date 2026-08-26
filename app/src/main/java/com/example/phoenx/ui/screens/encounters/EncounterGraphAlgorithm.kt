package com.example.phoenx.ui.screens.encounters

import com.example.phoenx.data.local.PersonEntity
import kotlin.math.abs

/**
 * EncounterGraphAlgorithm (v9.5.2 - ÉTAPE A)
 * Positionnement temporel des rencontres.
 * Y = Âge (relatif au minAge)
 * X = Centre + évitement de collisions
 */

data class EncounterNode(
    val person: PersonEntity,
    val x: Float, // en dp, 0 = centre
    val y: Float  // en dp, 0 = haut (marge incluse)
)

data class EncounterLayout(
    val nodes: List<EncounterNode>,
    val totalHeightDp: Float
)

object EncounterGraphAlgorithm {
    private const val DP_PER_YEAR = 60f 
    private const val MARGIN_TOP_DP = 40f
    private const val NODE_X_SPACING_DP = 150f

    fun calculateLayout(encounters: List<PersonEntity>): EncounterLayout {
        if (encounters.isEmpty()) return EncounterLayout(emptyList(), 0f)

        val minAge = encounters.mapNotNull { it.encounterAge }.minOrNull() ?: 0
        val nodes = mutableListOf<EncounterNode>()
        
        // Groupement par "étage" (proximité d'âge < 1 an = collision)
        val sorted = encounters.sortedBy { it.encounterAge ?: 0 }
        val rows = mutableListOf<MutableList<PersonEntity>>()
        
        sorted.forEach { person ->
            val age = person.encounterAge ?: 0
            val existingRow = rows.find { row -> 
                val rowAge = row.first().encounterAge ?: 0
                abs(rowAge - age) < 1 
            }
            if (existingRow != null) {
                existingRow.add(person)
            } else {
                rows.add(mutableListOf(person))
            }
        }

        rows.forEach { row ->
            val age = row.first().encounterAge ?: 0
            val y = (age - minAge) * DP_PER_YEAR + MARGIN_TOP_DP
            val startX = -(row.size - 1) * NODE_X_SPACING_DP / 2f
            
            row.forEachIndexed { index, person ->
                val x = startX + (index * NODE_X_SPACING_DP)
                nodes.add(EncounterNode(person, x, y))
            }
        }

        val maxAge = sorted.lastOrNull()?.encounterAge ?: 0
        val totalHeight = (maxAge - minAge) * DP_PER_YEAR + 200f

        // Logs de debug réclamés (tag PHOENX_GRAPH)
        android.util.Log.e("PHOENX_GRAPH", "Calcul Layout (ÉTAPE A.2) : $totalHeight dp")
        nodes.forEach { node ->
            android.util.Log.e("PHOENX_GRAPH", "Node: ${node.person.firstName}, X=${node.x}dp, Y=${node.y}dp")
        }

        return EncounterLayout(nodes, totalHeight)
    }
}
