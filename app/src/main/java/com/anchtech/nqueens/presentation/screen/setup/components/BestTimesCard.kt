package com.anchtech.nqueens.presentation.screen.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.anchtech.nqueens.R
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime
import com.anchtech.nqueens.presentation.theme.NQueensTheme

/**
 * Fastest solve per board size, with an empty state when there are none.
 */
@Composable
fun BestTimesCard(
    records: List<UiBestTime>,
    selectedSize: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_best_times),
                style = MaterialTheme.typography.titleMedium,
            )

            if (records.isEmpty()) {
                Text(
                    text = stringResource(R.string.setup_no_best_times),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                records.forEach { record ->
                    val highlighted = record.size == selectedSize
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.setup_size_label, record.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (highlighted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = record.time,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (highlighted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BestTimesCardPreview() {
    NQueensTheme {
        Surface {
            BestTimesCard(
                records = listOf(
                    UiBestTime(size = 4, time = "00:12"),
                    UiBestTime(size = 6, time = "01:14"),
                    UiBestTime(size = 8, time = "05:20"),
                    UiBestTime(size = 11, time = "21:24"),
                ),
                selectedSize = 8,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BestTimesCardEmptyPreview() {
    NQueensTheme {
        Surface {
            BestTimesCard(
                records = emptyList(),
                selectedSize = 8,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
