package com.example.phoenx.ui.screens.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.theme.AccentPrimary

@Composable
fun BookViewerScreen(
    navController: NavController,
    isRecipientMode: Boolean = false,
    viewModel: BookViewerViewModel = hiltViewModel()
) {
    val bookDraft by viewModel.bookDraft.collectAsState()
    val chapters = bookDraft?.chapters
        ?.sortedBy { it.orderIndex }
        ?: emptyList()

    if (chapters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1410)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = if (isRecipientMode)
                        "Le livre de vie n'a pas encore été rédigé."
                    else
                        "Ton livre n'est pas encore créé.",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF9B9590),
                        textAlign = TextAlign.Center
                    )
                )
                if (!isRecipientMode) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            navController.navigate("book_editor")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary
                        )
                    ) {
                        Text(
                            "Créer mon livre",
                            color = Color(0xFF1A1A1F)
                        )
                    }
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        pageCount = { chapters.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val chapter = chapters[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                Text(
                    text = "Chapitre ${chapter.orderIndex + 1}",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = Color(0xFF5C5855),
                        letterSpacing = 0.1.em
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = chapter.title,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontStyle = FontStyle.Italic,
                        color = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(AccentPrimary)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = chapter.content,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        color = Color(0xFFF2EDE8),
                        lineHeight = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1410),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = AccentPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF1C1410)
                        )
                    )
                )
                .padding(bottom = 24.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chapitre ${pagerState.currentPage + 1} / ${chapters.size}",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = AccentPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chapters.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(
                                if (index == pagerState.currentPage)
                                    8.dp else 5.dp
                            )
                            .background(
                                if (index == pagerState.currentPage)
                                    AccentPrimary
                                else
                                    Color(0xFF3E3E45),
                                CircleShape
                            )
                    )
                }
            }

            if (!isRecipientMode) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { navController.navigate("book_editor") }
                ) {
                    Text(
                        "Modifier ce livre",
                        color = Color(0xFF9B9590),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
