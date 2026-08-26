package com.anchtech.nqueens.presentation.screen.root

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.anchtech.nqueens.presentation.screen.game.gameScreen
import com.anchtech.nqueens.presentation.screen.game.navigateToGame
import com.anchtech.nqueens.presentation.screen.setup.SetupRoute
import com.anchtech.nqueens.presentation.screen.setup.setupScreen

/**
 * Owns the app's only `NavController`; destinations receive callbacks.
 */
@Composable
fun RootScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = SetupRoute) {
        setupScreen(
            onStartGame = { size -> navController.navigateToGame(size) },
        )
        gameScreen(
            onBackClick = { navController.navigateUp() },
        )
    }
}
