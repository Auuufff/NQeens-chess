package com.anchtech.nqueens.presentation.screen.setup

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * Start destination: choose a board size.
 */
@Serializable
data object SetupRoute

/**
 * Navigation is supplied as a callback; the screen holds no `NavController`.
 */
fun NavGraphBuilder.setupScreen(onStartGame: (Int) -> Unit) {
    composable<SetupRoute> {
        SetupScreen(onStartGame = onStartGame)
    }
}
