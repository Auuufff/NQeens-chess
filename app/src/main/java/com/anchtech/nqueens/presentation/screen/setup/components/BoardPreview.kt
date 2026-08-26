package com.anchtech.nqueens.presentation.screen.setup.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.anchtech.nqueens.R
import com.anchtech.nqueens.presentation.theme.NQueensTheme
import com.anchtech.nqueens.presentation.theme.boardColors
import kotlin.math.ceil

/**
 * An empty board at the given size.
 */
@Composable
fun BoardPreview(boardSize: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.boardColors
    val description = stringResource(R.string.setup_board_preview, boardSize)
    val animatedSize = animateFloatAsState(
        targetValue = boardSize.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "boardSize",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .semantics { contentDescription = description },
    ) {
        val current = animatedSize.value
        val cell = size.width / current
        val cells = ceil(current).toInt()

        // The light squares are the background, so only the dark half is drawn.
        drawRect(color = colors.lightSquare)

        for (row in 0 until cells) {
            for (col in 0 until cells) {
                if ((row + col) % 2 != 0) {
                    drawRect(
                        color = colors.darkSquare,
                        topLeft = Offset(x = col * cell, y = row * cell),
                        size = Size(width = cell, height = cell),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BoardPreviewSmallest() {
    NQueensTheme {
        Surface {
            BoardPreview(boardSize = 4, modifier = Modifier.padding(16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun BoardPreviewDefault() {
    NQueensTheme {
        Surface {
            BoardPreview(boardSize = 8, modifier = Modifier.padding(16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun BoardPreviewLargest() {
    NQueensTheme {
        Surface {
            BoardPreview(boardSize = 27, modifier = Modifier.padding(16.dp))
        }
    }
}
