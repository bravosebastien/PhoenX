package com.example.phoenx.domain.model

/**
 * Modèle de données résolu pour le rendu de l'Arbre Généalogique (v9.4.22)
 * Indépendant de la source (Créateur ou Destinataire).
 */
data class ResolvedPerson(
    val id: String,
    val firstName: String,
    val lastName: String?,
    val photoUrl: String?, // URL déjà résolue (signée ou locale)
    val isDeceased: Boolean,
    val biography: String,
    val parentIds: List<String>,
    val isReparented: Boolean = false, // v9.4.23
    val reparentedRelationLabel: String? = null // v9.4.23
)

/**
 * Un nœud de l'arbre positionné dans l'espace.
 */
data class VisualTreeNode(
    val person: ResolvedPerson,
    val generation: Int,
    val x: Float, // Position horizontale relative (0..1)
    val y: Float  // Position verticale relative (dérivée de la génération)
)

data class TreeLayout(
    val nodes: List<VisualTreeNode>,
    val connections: List<Pair<String, String>> // Liste de (ParentId, ChildId)
)
