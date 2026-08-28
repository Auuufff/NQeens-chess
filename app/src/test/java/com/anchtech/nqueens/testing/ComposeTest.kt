package com.anchtech.nqueens.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performTouchInput
import com.anchtech.nqueens.domain.model.Square
import com.anchtech.nqueens.presentation.theme.NQueensTheme

/**
 * Renders [content] in the app theme, pinned to the light scheme.
 */
fun ComposeContentTestRule.setScreen(content: @Composable () -> Unit) {
    setContent {
        NQueensTheme(darkTheme = false) {
            content()
        }
    }
}

/**
 * Taps the centre of [square] on a board node that is [boardSize] squares to a side.
 */
fun SemanticsNodeInteraction.clickSquare(square: Square, boardSize: Int) {
    performTouchInput {
        val cell = width / boardSize.toFloat()
        click(Offset(x = (square.col + 0.5f) * cell, y = (square.row + 0.5f) * cell))
    }
}
