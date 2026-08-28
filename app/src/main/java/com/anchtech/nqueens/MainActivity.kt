package com.anchtech.nqueens

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchtech.nqueens.presentation.screen.root.RootScreen
import com.anchtech.nqueens.presentation.screen.root.RootViewModel
import com.anchtech.nqueens.presentation.theme.NQueensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val storedDarkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
            val darkTheme = storedDarkTheme ?: isSystemInDarkTheme()

            LaunchedEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            NQueensTheme(darkTheme = darkTheme) {
                RootScreen()
            }
        }
    }
}
