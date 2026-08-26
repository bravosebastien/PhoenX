package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * EncounterGraphRenderer (v9.5.2 - ÉTAPE A)
 * Rendu squelette : Rectangles gris positionnés en DP avec défilement.
 */
@Composable
fun EncounterGraphRenderer(
    layout: EncounterLayout,
    onPersonClick: (PersonEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.totalHeightDp.dp)
        ) {
            val centerX = this.maxWidth / 2

            layout.nodes.forEach { node ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = centerX + node.x.dp - 50.dp, // -50dp pour centrer le rectangle de 100dp
                            y = node.y.dp
                        )
                        .size(100.dp, 60.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                        .border(1.dp, theme.contentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.person.firstName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
