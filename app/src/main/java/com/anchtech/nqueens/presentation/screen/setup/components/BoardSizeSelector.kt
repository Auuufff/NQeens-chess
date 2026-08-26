package com.anchtech.nqueens.presentation.screen.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.anchtech.nqueens.R
import com.anchtech.nqueens.presentation.theme.NQueensTheme
import kotlin.math.roundToInt

/**
 * Board size selection, with the resulting queen count.
 */
@Composable
fun BoardSizeSelector(
    sizes: IntRange,
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.setup_size_label, selectedSize)
    val valueRange = remember(sizes) { sizes.first.toFloat()..sizes.last.toFloat() }
    val steps = remember(sizes) { (sizes.count() - 2).coerceAtLeast(0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.setup_board_size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = label,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Slider(
            value = selectedSize.toFloat(),
            onValueChange = { value ->
                val next = value.roundToInt()
                if (next != selectedSize) onSizeSelected(next)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = label },
        )
    }
}

@PreviewLightDark
@Composable
private fun BoardSizeSelectorPreview() {
    NQueensTheme {
        Surface {
            BoardSizeSelector(
                sizes = 4..12,
                selectedSize = 8,
                onSizeSelected = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
