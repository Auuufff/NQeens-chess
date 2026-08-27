package com.anchtech.nqueens.presentation.screen.game

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * Route for the game screen. The board size travels as its only argument.
 */
@Serializable
data class GameRoute(val size: Int)

fun NavController.navigateToGame(size: Int) {
    navigate(GameRoute(size)) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.gameScreen(onBackClick: () -> Unit) {
    composable<GameRoute> {
        GameScreen(onBackClick = onBackClick)
    }
}
