package com.anchtech.nqueens.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Design-time only: renders the palette and a sample board so the theme can be judged
 * without running the app.
 */
@Composable
private fun ThemeShowcase() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("N-Queens", style = MaterialTheme.typography.headlineMedium)
            Text("02:14", style = TimerDisplay, color = MaterialTheme.colorScheme.primary)

            SampleBoard(size = 6)

            SwatchRow(
                "primary" to MaterialTheme.colorScheme.primary,
                "secondary" to MaterialTheme.colorScheme.secondary,
                "tertiary" to MaterialTheme.colorScheme.tertiary,
                "error" to MaterialTheme.colorScheme.error,
            )
            SwatchRow(
                "light sq" to MaterialTheme.boardColors.lightSquare,
                "dark sq" to MaterialTheme.boardColors.darkSquare,
                "conflict" to MaterialTheme.boardColors.darkSquareConflict,
                "victory" to MaterialTheme.boardColors.victory,
            )
        }
    }
}

/** A 6×6 board with two queens placed safely and two sharing a diagonal. */
@Composable
private fun SampleBoard(size: Int) {
    val board = MaterialTheme.boardColors
    val queens = setOf(0 to 1, 1 to 3, 3 to 0, 4 to 1)
    val conflicts = setOf(3 to 0, 4 to 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        repeat(size) { row ->
            Row(Modifier.weight(1f)) {
                repeat(size) { col ->
                    val isConflict = (row to col) in conflicts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(board.squareAt(row, col, isConflict))
                            .then(
                                if (isConflict) {
                                    Modifier.border(2.dp, board.queenOn(row, col))
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if ((row to col) in queens) {
                            Text(
                                text = "♛",
                                color = board.queenOn(row, col),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchRow(vararg swatches: Pair<String, Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { (label, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun ThemeShowcaseLightPreview() {
    NQueensTheme(darkTheme = false) { ThemeShowcase() }
}

@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun ThemeShowcaseDarkPreview() {
    NQueensTheme(darkTheme = true) { ThemeShowcase() }
}
