package com.anchtech.nqueens.presentation.screen.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchtech.nqueens.R
import com.anchtech.nqueens.common.extension.collectAsEffect
import com.anchtech.nqueens.common.extension.rememberSingleClick
import com.anchtech.nqueens.presentation.screen.setup.components.BestTimesCard
import com.anchtech.nqueens.presentation.screen.setup.components.BoardPreview
import com.anchtech.nqueens.presentation.screen.setup.components.BoardSizeSelector
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime
import com.anchtech.nqueens.presentation.theme.NQueensTheme

@Composable
internal fun SetupScreen(
    onStartGame: (Int) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.action.collectAsEffect { action ->
        when (action) {
            is SetupAction.StartGame -> onStartGame(action.size)
        }
    }

    SetupScreenContent(state = state)
}

@Composable
private fun SetupScreenContent(state: SetupState) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BoardPreview(boardSize = state.selectedSize)

            BoardSizeSelector(
                sizes = state.sizes,
                selectedSize = state.selectedSize,
                onSizeSelected = state.onSizeSelected,
            )

            Button(
                onClick = rememberSingleClick(state.onStartClick),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.setup_start),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            BestTimesCard(
                records = state.records,
                selectedSize = state.selectedSize,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SetupScreenPreview() {
    NQueensTheme {
        SetupScreenContent(
            state = SetupState(
                selectedSize = 8,
                records = listOf(
                    UiBestTime(size = 4, time = "00:12"),
                    UiBestTime(size = 6, time = "01:14"),
                    UiBestTime(size = 8, time = "05:20"),
                ),
            ),
        )
    }
}
