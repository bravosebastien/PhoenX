package com.example.phoenx.domain.genealogy

import com.example.phoenx.domain.model.ResolvedPerson
import com.example.phoenx.domain.model.TreeLayout
import com.example.phoenx.domain.model.VisualTreeNode

/**
 * Algorithme de positionnement de l'Arbre Généalogique (v9.4.29)
 * Gère le positionnement horizontal intelligent (fratries, demi-frères, conjoints)
 * et les liens de couple réels.
 */
object TreeAlgorithm {

    private const val NODE_WIDTH_SPACING = 200f // Largeur occupée par un nœud + espacement
    private const val GEN_HEIGHT_SPACING = 280f // Hauteur entre deux générations

    fun calculateLayout(persons: List<ResolvedPerson>): TreeLayout {
        if (persons.isEmpty()) return TreeLayout(emptyList(), emptyList())

        // --- PHASE 1 : Calcul des générations (Niveaux) ---
        val parentMap = mutableMapOf<String, String>()
        fun find(id: String): String {
            if (parentMap[id] == null || parentMap[id] == id) return id.also { parentMap[it] = it }
            parentMap[id] = find(parentMap[id]!!)
            return parentMap[id]!!
        }
        fun union(id1: String, id2: String) {
            val root1 = find(id1)
            val root2 = find(id2)
            if (root1 != root2) parentMap[root1] = root2
        }

        persons.forEach { person ->
            val knownParents = person.parentIds.filter { pid -> persons.any { it.id == pid } }
            if (knownParents.size >= 2) {
                union(knownParents[0], knownParents[1])
            }
        }

        val groupLevels = mutableMapOf<String, Int>()
        var changed = true
        var safety = 0
        while (changed && safety < 100) {
            changed = false
            safety++
            persons.forEach { person ->
                val myGroupId = find(person.id)
                val knownParents = person.parentIds.filter { pid -> persons.any { it.id == pid } }
                val currentGroupLevel = groupLevels[myGroupId] ?: -1
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

        val nodeGenerations = persons.associate { it.id to (groupLevels[find(it.id)] ?: 0) }
        val levels = nodeGenerations.values.distinct().sorted()

        // --- PHASE 2 : Construction des liens de couple RÉELS ---
        val coupleConnections = mutableListOf<Pair<String, String>>()
        persons.forEach { child ->
            val parentsInTree = child.parentIds.filter { pid -> persons.any { it.id == pid } }
            if (parentsInTree.size == 2) {
                val p1 = parentsInTree[0]
                val p2 = parentsInTree[1]
                if (!coupleConnections.any { (it.first == p1 && it.second == p2) || (it.first == p2 && it.second == p1) }) {
                    coupleConnections.add(p1 to p2)
                }
            }
        }

        // --- PHASE 3 : Positionnement Horizontal Intelligent (Calcul des X) ---
        val visualNodes = mutableListOf<VisualTreeNode>()
        val connections = mutableListOf<Pair<List<String>, String>>()
        val orderedPersonsPerLevel = mutableMapOf<Int, List<ResolvedPerson>>()

        levels.forEach { level ->
            val personsAtLevel = persons.filter { nodeGenerations[it.id] == level }
            
            val orderedList = if (level == 0) {
                // Pour la racine, on garde l'ordre groupé par couple puis alpha
                personsAtLevel.sortedWith(compareBy({ find(it.id) }, { it.firstName }))
            } else {
                val previousLevelOrder = orderedPersonsPerLevel[level - 1] ?: emptyList()
                val processedIds = mutableSetOf<String>()
                val levelOrdered = mutableListOf<ResolvedPerson>()

                // 1. On suit l'ordre des parents de la génération précédente
                previousLevelOrder.forEach { parent ->
                    val childrenOfParent = personsAtLevel.filter { it.parentIds.contains(parent.id) && !processedIds.contains(it.id) }
                    
                    // Groupement par "fratrie" (ceux qui partagent exactement les mêmes parents présents)
                    val siblingsGrouped = childrenOfParent.groupBy { child ->
                        child.parentIds.filter { pid -> persons.any { it.id == pid } }.sorted().joinToString(",")
                    }

                    siblingsGrouped.forEach { (_, siblingGroup) ->
                        val group = siblingGroup.sortedBy { it.firstName }
                        
                        // On ajoute d'abord toute la fratrie "de sang"
                        group.forEach { child ->
                            if (processedIds.add(child.id)) {
                                levelOrdered.add(child)
                            }
                        }

                        // Puis on ajoute les conjoints de cette fratrie sur le côté EXTÉRIEUR (après le bloc)
                        group.forEach { child ->
                            val spouse = personsAtLevel.find { p ->
                                !processedIds.contains(p.id) && 
                                p.parentIds.none { pid -> persons.any { it.id == pid } } &&
                                coupleConnections.any { (it.first == child.id && it.second == p.id) || (it.first == p.id && it.second == child.id) }
                            }
                            spouse?.let { 
                                if (processedIds.add(it.id)) levelOrdered.add(it)
                            }
                        }
                    }
                }
                
                // 3. On ajoute ceux qui resteraient (cas isolés ou racines secondaires à ce niveau)
                personsAtLevel.filter { !processedIds.contains(it.id) }
                    .sortedBy { it.firstName }
                    .forEach { levelOrdered.add(it) }

                levelOrdered
            }
            
            orderedPersonsPerLevel[level] = orderedList

            val totalWidth = (orderedList.size - 1) * NODE_WIDTH_SPACING
            val startX = -totalWidth / 2f

            orderedList.forEachIndexed { index, person ->
                val x = startX + (index * NODE_WIDTH_SPACING)
                val y = level * GEN_HEIGHT_SPACING
                
                visualNodes.add(VisualTreeNode(
                    person = person, 
                    generation = level, 
                    x = x, 
                    y = y, 
                    groupId = find(person.id)
                ))
            }
        }

        // Connexions basées sur les parents RÉELS pour le midpoint
        persons.forEach { child ->
            val realParents = child.parentIds.filter { pid -> persons.any { it.id == pid } }
            if (realParents.isNotEmpty()) {
                connections.add(realParents to child.id)
            }
        }

        return TreeLayout(visualNodes, connections, coupleConnections)
    }
}
