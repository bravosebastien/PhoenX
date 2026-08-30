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
    val reparentedRelationLabel: String? = null, // v9.4.23
    val photoField: String = "imageUrl", // v9.6.7
    val localPath: String? = null // v9.6.7 : Persistance du chemin local
)

/**
 * Un nœud de l'arbre positionné dans l'espace.
 * v9.4.28 : x et y sont désormais en unités fixes (DP) pour un terrain infini.
 */
data class VisualTreeNode(
    val person: ResolvedPerson,
    val generation: Int,
    val x: Float, // Position horizontale (en DP)
    val y: Float, // Position verticale (en DP)
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
    val connections: List<Pair<List<String>, String>>, // v9.4.28 : (Liste des ParentIds réels, ChildId)
    val coupleConnections: List<Pair<String, String>> = emptyList() // v9.4.28 : Liens directs entre co-parents
)
