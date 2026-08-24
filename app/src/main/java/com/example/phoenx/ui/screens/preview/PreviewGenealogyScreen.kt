package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.screens.genealogy.GenealogyTreeRenderer
import com.example.phoenx.ui.screens.genealogy.GenealogyTreeViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewGenealogyScreen(
    recipientUid: String,
    onNavigateBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
    treeViewModel: GenealogyTreeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val treeLayout by treeViewModel.treeLayout.collectAsState()
    
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    LaunchedEffect(recipientUid) {
        viewModel.loadPreview(recipientUid)
        // L'arbre est public pour tous les destinataires une fois activé.
        // On affiche l'arbre complet du Créateur.
        treeViewModel.loadTree(null) 
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Arbre Généalogique (Aperçu)", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(state.recipientName, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.familyCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ton arbre est encore vide.", color = theme.contentColor.copy(alpha = 0.4f))
                }
            } else {
                // Rendu visuel de l'arbre (Lecture seule)
                GenealogyTreeRenderer(
                    layout = treeLayout,
                    onPersonClick = { /* Détails en lecture seule ? */ },
                    onAddChild = { /* Désactivé en aperçu */ },
                    creatorId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                    enabled = false // Mode lecture seule strict
                )
                
                // Petit indicateur flottant pour rappeler que c'est un aperçu
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                    color = accent.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Mode Aperçu : Lecture Seule",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = theme.backgroundColor
                    )
                }
            }
        }
    }
}
