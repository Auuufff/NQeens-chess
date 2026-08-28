package com.anchtech.nqueens.presentation.screen.setup

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchtech.nqueens.R
import com.anchtech.nqueens.common.extension.collectAsEffect
import com.anchtech.nqueens.common.extension.isLandscape
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
        if (isLandscape()) {
            SetupScreenLandscape(state = state)
        } else {
            SetupScreenPortrait(state = state)
        }
    }
}

@Composable
private fun SetupScreenPortrait(state: SetupState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SetupHeader()

        BoardPreview(boardSize = state.selectedSize)

        BoardSizeSelector(
            sizes = state.sizes,
            selectedSize = state.selectedSize,
            onSizeSelected = state.onSizeSelected,
        )

        StartButton(onClick = state.onStartClick)

        BestTimesCard(
            records = state.records,
            selectedSize = state.selectedSize,
        )
    }
}

@Composable
private fun SetupScreenLandscape(state: SetupState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoardPreview(
            boardSize = state.selectedSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SetupHeader()

            BoardSizeSelector(
                sizes = state.sizes,
                selectedSize = state.selectedSize,
                onSizeSelected = state.onSizeSelected,
            )

            StartButton(onClick = state.onStartClick)

            BestTimesCard(
                records = state.records,
                selectedSize = state.selectedSize,
            )
        }
    }
}

@Composable
private fun SetupHeader() {
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
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    Button(
        onClick = rememberSingleClick(onClick),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.setup_start),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 6.dp),
        )
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

@Preview(name = "Landscape", widthDp = 800, heightDp = 360)
@Preview(name = "Landscape dark", widthDp = 800, heightDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetupScreenLandscapePreview() {
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
