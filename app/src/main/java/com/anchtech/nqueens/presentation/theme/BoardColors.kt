package com.anchtech.nqueens.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Board colours, which Material's roles do not describe.
 *
 * Every queen-on-square pair clears 4.5:1, conflicting squares included.
 */
@Immutable
data class BoardColors(
    val lightSquare: Color,
    val darkSquare: Color,
    /**
     * Light square holding a conflicting queen.
     */
    val lightSquareConflict: Color,
    /**
     * Dark square holding a conflicting queen.
     */
    val darkSquareConflict: Color,
    /**
     * Queen on a light square.
     */
    val queenOnLight: Color,
    /**
     * Queen on a dark square.
     */
    val queenOnDark: Color,
    /**
     * Solved board and new-record badge. A fill colour, not a text colour.
     */
    val victory: Color,
    /**
     * Ink for content sitting on [victory]: the medal queen and the badge label.
     */
    val onVictory: Color,
    /**
     * Rank and file labels along the board edge.
     */
    val coordinate: Color,
) {
    /**
     * Background for the square at ([row], [col]).
     */
    fun squareAt(row: Int, col: Int, isConflicting: Boolean = false): Color = when {
        isLight(row, col) && isConflicting -> lightSquareConflict
        isLight(row, col) -> lightSquare
        isConflicting -> darkSquareConflict
        else -> darkSquare
    }

    /**
     * Queen colour for the square at ([row], [col]).
     */
    fun queenOn(row: Int, col: Int): Color =
        if (isLight(row, col)) queenOnLight else queenOnDark

    private fun isLight(row: Int, col: Int) = (row + col) % 2 == 0
}

internal val LightBoardColors = BoardColors(
    lightSquare = ParchmentSquare,
    darkSquare = SageSquare,
    lightSquareConflict = ConflictParchment,
    darkSquareConflict = ConflictSage,
    queenOnLight = QueenInk,
    queenOnDark = QueenIvory,
    victory = VictoryGold,
    onVictory = VictoryInk,
    coordinate = NeutralVariant50,
)

internal val DarkBoardColors = BoardColors(
    lightSquare = ParchmentSquareDark,
    darkSquare = SageSquareDark,
    lightSquareConflict = ConflictParchmentDark,
    darkSquareConflict = ConflictSageDark,
    queenOnLight = QueenInk,
    queenOnDark = QueenIvory,
    victory = VictoryGold,
    onVictory = VictoryInk,
    coordinate = NeutralVariant60,
)

internal val LocalBoardColors = staticCompositionLocalOf { LightBoardColors }

/**
 * Board palette for the current theme: `MaterialTheme.boardColors`.
 */
val MaterialTheme.boardColors: BoardColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBoardColors.current
