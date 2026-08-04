package com.example.phoenx.domain.genealogy

import com.example.phoenx.domain.model.ResolvedPerson
import com.example.phoenx.domain.model.TreeLayout
import com.example.phoenx.domain.model.VisualTreeNode

/**
 * Algorithme de positionnement de l'Arbre Généalogique (v9.4.26)
 * Utilise Union-Find pour aligner les co-parents sur le même niveau.
 */
object TreeAlgorithm {

    fun calculateLayout(persons: List<ResolvedPerson>): TreeLayout {
        if (persons.isEmpty()) return TreeLayout(emptyList(), emptyList())

        // --- PHASE 1 : Union-Find pour regrouper les co-parents ---
        val parent = mutableMapOf<String, String>()
        fun find(id: String): String {
            if (parent[id] == null || parent[id] == id) return id.also { parent[it] = it }
            parent[id] = find(parent[id]!!)
            return parent[id]!!
        }
        fun union(id1: String, id2: String) {
            val root1 = find(id1)
            val root2 = find(id2)
            if (root1 != root2) parent[root1] = root2
        }

        // Pour chaque enfant, on unit ses deux parents dans le même groupe
        persons.forEach { person ->
            val knownParents = person.parentIds.filter { pid -> persons.any { it.id == pid } }
            if (knownParents.size >= 2) {
                union(knownParents[0], knownParents[1])
            }
        }

        // --- PHASE 2 : Calcul des niveaux par GROUPE ---
        val groupLevels = mutableMapOf<String, Int>() // groupId -> level
        
        // Stabilisation topologique (chemin le plus long pour satisfaire toutes les contraintes)
        var changed = true
        var safety = 0
        while (changed && safety < 100) {
            changed = false
            safety++
            persons.forEach { person ->
                val myGroupId = find(person.id)
                val knownParents = person.parentIds.filter { pid -> persons.any { it.id == pid } }
                
                // Correction sentinelle (-1 au lieu de 0) pour permettre l'écriture du niveau racine 0
                val currentGroupLevel = groupLevels[myGroupId] ?: -1
                
                // Le niveau du groupe doit être supérieur au niveau max de tous les parents de tous les membres du groupe
                val parentMaxLevel = knownParents.map { find(it) }
                    .mapNotNull { groupLevels[it] }
                    .maxOrNull() ?: -1
                
                val targetLevel = parentMaxLevel + 1
                if (targetLevel > currentGroupLevel) {
                    groupLevels[myGroupId] = targetLevel
                    changed = true
                }
            }
        }

        // --- PHASE 3 : Attribution des coordonnées X et Y ---
        val nodeGenerations = persons.associate { it.id to (groupLevels[find(it.id)] ?: 0) }
        val maxGen = (nodeGenerations.values.maxOrNull() ?: 0).coerceAtLeast(1)
        
        val visualNodes = mutableListOf<VisualTreeNode>()
        val connections = mutableListOf<Pair<String, String>>()
        val levels = nodeGenerations.values.distinct().sorted()

        levels.forEach { level ->
            val personsAtLevel = persons.filter { nodeGenerations[it.id] == level }
                .sortedBy { it.firstName } // Tri alpha pour stabilité
            
            personsAtLevel.forEachIndexed { index, person ->
                // X : Distribution horizontale équilibrée (0..1)
                val x = (index + 1).toFloat() / (personsAtLevel.size + 1)
                // Y : Position verticale (0..1) basée sur la génération
                val y = (level + 0.5f) / (maxGen + 1)
                
                visualNodes.add(VisualTreeNode(
                    person = person, 
                    generation = level, 
                    x = x, 
                    y = y, 
                    groupId = find(person.id) // v9.4.26
                ))
                
                // Enregistrement des liens pour le Canvas
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
