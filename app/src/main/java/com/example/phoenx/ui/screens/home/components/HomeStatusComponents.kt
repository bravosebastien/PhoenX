package com.example.phoenx.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.AppThemeState
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun StatusBadge(title: String, subtitle: String, dotColor: Color, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape).shadow(4.dp, CircleShape, spotColor = dotColor))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    title, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = theme.fontFamily
                    ), 
                    color = theme.contentColor
                )
                Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun LastMemoryCard(entry: com.example.phoenx.data.local.OfflineEntry?) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Card(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(accent, Color.Transparent))))
            Column(modifier = Modifier.padding(13.dp, 14.dp)) {
                Text(
                    "DERNIER SOUVENIR", 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), 
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (entry == null) {
                    Text(
                        "Aucun souvenir déposé pour l'instant.", 
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic, 
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp
                        ), 
                        color = theme.contentColor.copy(alpha = 0.7f)
                    )
                    Text("— Commence dès maintenant", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                } else {
                    Text(
                        entry.aiSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic, 
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp
                        ),
                        color = theme.contentColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val age = com.example.phoenx.domain.util.AgeUtils.parseAgeJson(entry.ageAtCreation)
                    Text("— À ${age.years} ans", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ProgressionCard(memoriesCount: Int, questionsCount: Int, chaptersCount: Int) {
    val theme = LocalAppTheme.current
    Card(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(count = memoriesCount, label = "SOUVENIRS", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.contentColor.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
            StatItem(count = questionsCount, label = "QUESTIONS", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.contentColor.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
            StatItem(count = chaptersCount, label = "CHAPITRES", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(), 
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = theme.fontFamily, 
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ), 
            color = accent
        )
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), 
            color = theme.contentColor.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    name: String, 
    modifier: Modifier = Modifier,
    badgeCount: Int = 0, 
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(accent.copy(alpha = 0.4f)))
            
            if (badgeCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp),
                    shape = CircleShape,
                    color = accent
                ) {
                    Text(badgeCount.toString(), color = theme.backgroundColor, fontSize = 8.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    name, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = theme.fontFamily,
                        fontStyle = FontStyle.Italic, 
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ), 
                    color = theme.contentColor.copy(alpha = 0.8f), 
                    textAlign = TextAlign.Center
                )
            }
            
            // Halo bottom
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).background(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.05f)))))
        }
    }
}

@Composable
fun TrustCircleCard(
    onClick: () -> Unit,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(110.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.People, null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Mon Cercle de Confiance",
                    style = TextStyle(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = theme.contentColor
                )
                Text(
                    "Gérer mes héritiers et dépositaires",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = theme.contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
