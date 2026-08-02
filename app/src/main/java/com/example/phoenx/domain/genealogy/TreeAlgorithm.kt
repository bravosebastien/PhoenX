package com.example.phoenx.domain.genealogy

import com.example.phoenx.domain.model.ResolvedPerson
import com.example.phoenx.domain.model.TreeLayout
import com.example.phoenx.domain.model.VisualTreeNode

/**
 * Algorithme de positionnement de l'Arbre Généalogique (Lot 1 - v9.4.22)
 * Calcule les générations et les coordonnées relatives (0..1) pour le rendu Canvas.
 */
object TreeAlgorithm {

    fun calculateLayout(persons: List<ResolvedPerson>): TreeLayout {
        if (persons.isEmpty()) return TreeLayout(emptyList(), emptyList())

        val nodeGenerations = mutableMapOf<String, Int>() // personId -> generation level
        
        // 1. Calcul des niveaux de génération
        // On boucle jusqu'à stabilisation (gestion des liens complexes)
        var changed = true
        var safetyBreak = 0
        while (changed && safetyBreak < 100) {
            changed = false
            safetyBreak++
            for (person in persons) {
                val currentLevel = nodeGenerations[person.id]
                
                // Un nœud est une racine s'il n'a aucun parent connu dans la liste
                val knownParents = person.parentIds.filter { pid -> persons.any { it.id == pid } }
                
                val newLevel = if (knownParents.isEmpty()) {
                    0
                } else {
                    val parentLevels = knownParents.mapNotNull { nodeGenerations[it] }
                    if (parentLevels.size < knownParents.size) {
                        // Certains parents ne sont pas encore calculés, on attend
                        currentLevel ?: -1 
                    } else {
                        (parentLevels.maxOrNull() ?: 0) + 1
                    }
                }

                if (newLevel != currentLevel && newLevel != -1) {
                    nodeGenerations[person.id] = newLevel
                    changed = true
                }
            }
        }

        // Pour les orphelins ou cycles restants, on force le niveau 0
        persons.forEach { if (nodeGenerations[it.id] == null) nodeGenerations[it.id] = 0 }

        // 2. Attribution des coordonnées X et Y
        val visualNodes = mutableListOf<VisualTreeNode>()
        val connections = mutableListOf<Pair<String, String>>()
        
        val maxGen = (nodeGenerations.values.maxOrNull() ?: 0).coerceAtLeast(1)
        val levels = nodeGenerations.values.distinct().sorted()

        levels.forEach { level ->
            val personsAtLevel = persons.filter { nodeGenerations[it.id] == level }
                .sortedBy { it.firstName } // Tri alpha pour stabilité visuelle
            
            personsAtLevel.forEachIndexed { index, person ->
                // X : Distribution horizontale équilibrée (0..1)
                val x = (index + 1).toFloat() / (personsAtLevel.size + 1)
                // Y : Position verticale (0..1) basée sur la profondeur
                val y = level.toFloat() / maxGen
                
                visualNodes.add(VisualTreeNode(person, level, x, y))
                
                // Enregistrement des liens
                person.parentIds.forEach { parentId ->
                    if (persons.any { it.id == parentId }) {
                        connections.add(parentId to person.id)
                    }
                }
            }
        }

        return TreeLayout(visualNodes, connections)
    }
}
