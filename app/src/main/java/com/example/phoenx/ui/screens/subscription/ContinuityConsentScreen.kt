package com.example.phoenx.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush

/**
 * LOT G : Écran de consentement explicite pour le palier "Continuité".
 * SCAFFOLDING SEULEMENT : Ne déclenche aucun prélèvement RevenueCat sans validation active.
 * Si le Créateur refuse ou ignore, application de la règle ordinaire de rétrogradation (LOT C).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinuityConsentScreen(
    onAcceptContinuity: () -> Unit,
    onDeclineToStandardDowngrade: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transition Continuité",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = theme.contentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = theme.contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Conserver tous vos destinataires actifs",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = theme.contentColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "À la fin de votre abonnement Prestige, le palier « Continuité » (22,99 € / mois) vous permet de maintenir l'ensemble de vos destinataires et dépositaires actifs sans aucune mise en pause.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accent.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✓ Aucun destinataire ni dépositaire mis en pause",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                        Text(
                            text = "✓ Conservation intégrale de vos médias déjà crédités",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                        Text(
                            text = "ℹ️ Seuil d'ajout de nouveaux contenus : aligné sur Lignée",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAcceptContinuity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = theme.backgroundColor
                    )
                ) {
                    Text(
                        text = "Valider l'option Continuité (22,99 € / mois)",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDeclineToStandardDowngrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Refuser (Ajuster mes destinataires actifs)",
                        color = theme.contentColor
                    )
                }
            }
        }
    }
}
