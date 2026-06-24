package com.example.phoenx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.phoenx.ui.navigation.PhoenXNavGraph
import com.example.phoenx.ui.theme.PhoenXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoenXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.phoenx.ui.theme.BackgroundPrimary
                ) {
                    val navController = rememberNavController()
                    PhoenXNavGraph(navController = navController)
                }
            }
        }
    }
}
