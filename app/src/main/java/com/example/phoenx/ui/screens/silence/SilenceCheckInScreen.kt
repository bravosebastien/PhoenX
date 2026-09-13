package com.example.phoenx.ui.screens.silence

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.phoenx.R
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilenceCheckInScreen(
    onImHere: () -> Unit,
    onTraversingSomething: (String) -> Unit
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Point pulsant animé
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(pulseScale)
                    .background(Color(0xFFC97B3A), CircleShape)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.silence_checkin_title),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFFF2EDE8),
                    textAlign = TextAlign.Center
                )
                InfoButton(
                    title = stringResource(R.string.silence_checkin_info_title),
                    points = listOf(
                        stringResource(R.string.silence_checkin_info_p1),
                        stringResource(R.string.silence_checkin_info_p2),
                        stringResource(R.string.silence_checkin_info_p3),
                        stringResource(R.string.silence_checkin_info_p4),
                        stringResource(R.string.silence_checkin_info_p5)
                    )
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = {
                    onImHere()
                    Toast.makeText(context, context.getString(R.string.silence_checkin_toast_noted), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC97B3A))
            ) {
                Text(stringResource(R.string.silence_button_im_here), color = Color(0xFF1A1A1F), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC97B3A)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC97B3A))
            ) {
                Text(stringResource(R.string.silence_button_traversing))
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1A1F),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2E2E35)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.silence_traversing_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFFF2EDE8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.silence_traversing_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9B9590),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    TraversingOptionCard(
                        icon = "🎙️",
                        title = stringResource(R.string.silence_traversing_option_record),
                        onClick = {
                            onTraversingSomething("record")
                            showBottomSheet = false
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TraversingOptionCard(
                        icon = "💭",
                        title = stringResource(R.string.silence_traversing_option_pass),
                        onClick = {
                            onTraversingSomething("pass")
                            showBottomSheet = false
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = { showBottomSheet = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.silence_button_close), color = Color(0xFF9B9590))
                    }
                }
            }
        }
    }
}

@Composable
fun TraversingOptionCard(icon: String, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E35)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color(0xFFF2EDE8), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
