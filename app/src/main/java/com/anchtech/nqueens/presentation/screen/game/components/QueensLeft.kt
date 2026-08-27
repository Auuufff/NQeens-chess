package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.anchtech.nqueens.R

/**
 * How many queens are still to be placed.
 *
 * Only the count animates. Wrapping the whole sentence reads as the line being replaced,
 * which is louder than the one thing that actually changed.
 */
@Composable
internal fun QueensLeft(count: Int, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val label = stringResource(R.string.game_queens_left)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$count $label" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val down = targetState < initialState
                (slideInVertically { if (down) it else -it })
                    .togetherWith(slideOutVertically { if (down) -it else it })
            },
            label = "count",
        ) { value ->
            Text(text = "$value", style = style, color = color)
        }

        Text(text = label, style = style, color = color)
    }
}
