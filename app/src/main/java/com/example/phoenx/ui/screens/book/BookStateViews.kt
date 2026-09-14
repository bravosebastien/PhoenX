package com.example.phoenx.ui.screens.book

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.phoenx.R
import com.example.phoenx.data.model.ChapterRegenStatus
import com.example.phoenx.data.model.RegenerationDashboard
import com.example.phoenx.ui.theme.AppThemeState
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere

@Composable
fun EmptyBookState(
    entryCount: Int,
    onGenerate: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val canGenerate = entryCount >= 10 // v9.3.1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawRect(
                color = accent,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawRect(
                color = accent,
                topLeft = Offset(w * 0.52f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawLine(
                color = accent,
                start = Offset(w * 0.5f, h * 0.1f),
                end = Offset(w * 0.5f, h * 0.9f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.4f),
                    start = Offset(w * 0.16f, y),
                    end = Offset(w * 0.44f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.4f),
                    start = Offset(w * 0.56f, y),
                    end = Offset(w * 0.84f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.book_empty_state_title),
            style = TextStyle(
                fontFamily = theme.fontFamily,
                fontSize = 20.sp,
                color = theme.contentColor,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.book_empty_state_desc),
            style = TextStyle(
                fontSize = 14.sp,
                color = theme.contentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onGenerate,
            enabled = canGenerate, // v9.3.1
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canGenerate) accent else theme.contentColor.copy(alpha = 0.1f),
                contentColor = if (canGenerate) theme.backgroundColor else theme.contentColor.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (canGenerate) stringResource(R.string.book_empty_state_btn_generate) else stringResource(R.string.book_empty_state_missing_memories, 10 - entryCount),
                style = TextStyle(
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
fun ProposedPlanView(
    plan: List<Map<String, Any?>>,
    onValidate: () -> Unit,
    onCancel: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Architecture, null, tint = accent, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.book_proposed_plan_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = theme.contentColor)
        )
        Text(
            stringResource(R.string.book_proposed_plan_desc),
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(32.dp))
        
        plan.forEachIndexed { index, chapter ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.book_proposed_plan_chapter_header, index + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Text(
                        chapter["title"] as? String ?: stringResource(R.string.book_proposed_plan_no_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    chapter["description"]?.let {
                        Text(
                            it as String,
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    val scenes = chapter["sceneIds"] as? List<*>
                    Text(
                        stringResource(R.string.book_proposed_plan_memories_count, scenes?.size ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onValidate,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text(stringResource(R.string.book_proposed_plan_btn_confirm), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.book_proposed_plan_btn_cancel), color = theme.contentColor.copy(alpha = 0.6f))
        }
    }
}

/**
 * Lot 2 : Tableau de bord de régénération. Affiche l'état de chaque chapitre (intact / à retravailler
 * + raison en clair) et les souvenirs pas encore intégrés à un chapitre. Comparaison 100% locale et
 * gratuite — cet écran ne déclenche aucun appel IA. Le bouton en bas continue de régénérer
 * l'INTÉGRALITÉ du livre, exactement comme avant (comportement inchangé, le Lot 3 fera l'envoi
 * ciblé des seuls chapitres cochés).
 */
@Composable
fun RegenerationDashboardView(
    dashboard: RegenerationDashboard,
    onConfirmRegenerate: () -> Unit,
    onCancel: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val intactColor = Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.FactCheck, null, tint = accent, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.book_regen_dashboard_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = theme.contentColor)
        )
        Text(
            stringResource(R.string.book_regen_dashboard_desc),
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        dashboard.globalReason?.let { reason ->
            Spacer(Modifier.height(16.dp))
            Surface(
                color = accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        dashboard.chapters.forEach { chapterInfo ->
            val isIntact = chapterInfo.status == ChapterRegenStatus.INTACT
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isIntact) Icons.Default.CheckCircle else Icons.Default.EditNote,
                            contentDescription = null,
                            tint = if (isIntact) intactColor else accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        val statusLabel = if (isIntact) stringResource(R.string.book_regen_status_intact) else stringResource(R.string.book_regen_status_rework)
                        Text(
                            stringResource(R.string.book_regen_dashboard_chapter_status, chapterInfo.orderIndex + 1, statusLabel),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isIntact) intactColor else accent
                        )
                    }
                    Text(
                        chapterInfo.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (chapterInfo.reasons.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            chapterInfo.reasons.forEach { reason ->
                                Text(
                                    "• $reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dashboard.orphanScenes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.book_regen_dashboard_orphan_title, dashboard.orphanScenes.size),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = theme.contentColor,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            )
            dashboard.orphanScenes.forEach { orphan ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.NewReleases, null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "« ${orphan.summary} »",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            stringResource(R.string.book_regen_dashboard_warning_full),
            style = MaterialTheme.typography.labelSmall,
            color = theme.contentColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onConfirmRegenerate,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text(stringResource(R.string.book_regen_dashboard_btn_regenerate), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.book_proposed_plan_btn_cancel), color = theme.contentColor.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun GeneratingBookState(progress: String) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Canvas(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
        ) {
            drawCircle(
                color = accent.copy(alpha = alpha * 0.2f),
                radius = size.width / 2f
            )
            drawCircle(
                color = accent.copy(alpha = alpha),
                radius = size.width / 3f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = accent.copy(alpha = alpha),
                start = Offset(size.width / 2f, size.height * 0.25f),
                end = Offset(size.width / 2f, size.height * 0.75f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        val textAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "textAlpha"
        )

        Text(
            text = progress,
            style = TextStyle(
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = theme.contentColor.copy(alpha = textAlpha),
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.book_generating_moments),
            style = TextStyle(
                fontSize = 12.sp,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun BookAiExplanationDialog(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Psychology, null, tint = accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.book_ai_explanation_title), 
                    color = theme.contentColor, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.EditNote, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.book_ai_explanation_edit_chapter_title), style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.book_ai_explanation_edit_chapter_desc), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.book_ai_explanation_regen_book_title), style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.book_ai_explanation_regen_book_desc), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.book_ai_explanation_btn_clear), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun BookOnboardingDialog(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.AutoStories, 
                    null, 
                    tint = accent, 
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.book_onboarding_title), 
                    color = theme.contentColor, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnboardingPoint(
                    icon = Icons.Default.Timeline,
                    title = stringResource(R.string.book_onboarding_chrono_title),
                    description = stringResource(R.string.book_onboarding_chrono_desc),
                    theme = theme
                )
                OnboardingPoint(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.book_onboarding_chars_title),
                    description = stringResource(R.string.book_onboarding_chars_desc),
                    theme = theme
                )
                OnboardingPoint(
                    icon = Icons.Default.HistoryEdu,
                    title = stringResource(R.string.book_onboarding_narration_title),
                    description = stringResource(R.string.book_onboarding_narration_desc),
                    theme = theme
                )
                
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.book_onboarding_advice_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.book_onboarding_advice_content),
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.book_onboarding_btn_understood), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OnboardingPoint(
    icon: ImageVector,
    title: String,
    description: String,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f), lineHeight = 18.sp)
        }
    }
}
