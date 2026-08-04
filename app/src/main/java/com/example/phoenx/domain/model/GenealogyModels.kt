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
    val y: Float, // Position verticale relative (dérivée de la génération)
    val groupId: String // v9.4.26 : ID du groupe de co-parenté
)

/**
 * Modèle pour l'affichage en liste hiérarchique par groupes (v9.4.26)
 */
data class VisualGroup(
    val id: String,
    val members: List<ResolvedPerson>,
    val level: Int,
    val children: List<VisualGroup> = emptyList()
)

data class TreeLayout(
    val nodes: List<VisualTreeNode>,
    val connections: List<Pair<String, String>> // Liste de (ParentId, ChildId)
)
