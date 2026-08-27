package com.anchtech.nqueens.presentation.screen.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.anchtech.nqueens.R
import com.anchtech.nqueens.common.extension.rememberSingleClick
import com.anchtech.nqueens.presentation.theme.NQueensTheme
import com.anchtech.nqueens.presentation.theme.TimerDisplay
import com.anchtech.nqueens.presentation.theme.boardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VictoryOverlay(
    visible: Boolean,
    time: String,
    isNewRecord: Boolean,
    onPlayAgainClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }

    BasicAlertDialog(
        onDismissRequest = onBackClick,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            VictoryPanel(
                time = time,
                isNewRecord = isNewRecord,
                onPlayAgainClick = onPlayAgainClick,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
private fun VictoryPanel(
    time: String,
    isNewRecord: Boolean,
    onPlayAgainClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 360.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            QueenMedal()

            VictorySummary(time = time, isNewRecord = isNewRecord)

            VictoryActions(
                onPlayAgainClick = onPlayAgainClick,
                onBackClick = onBackClick,
            )
        }
    }
}

/**
 * The headline, the frozen clock and — when it was earned — the record badge.
 */
@Composable
private fun VictorySummary(time: String, isNewRecord: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.game_solved),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.game_your_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = time, style = TimerDisplay)
        }

        if (isNewRecord) {
            NewRecordBadge()
        }
    }
}

@Composable
private fun VictoryActions(
    onPlayAgainClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = rememberSingleClick(onPlayAgainClick),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.game_play_again),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        TextButton(
            onClick = rememberSingleClick(onBackClick),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.game_back_to_menu))
        }
    }
}

@Composable
private fun QueenMedal(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.boardColors

    Box(
        modifier = modifier
            .size(72.dp)
            .background(color = colors.victory, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_queen),
            contentDescription = null,
            tint = colors.onVictory,
            modifier = Modifier.fillMaxSize(.7f),
        )
    }
}

@Composable
private fun NewRecordBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.boardColors.victory,
    ) {
        Text(
            text = stringResource(R.string.game_new_record),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.boardColors.onVictory,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun VictoryPanelPreview() {
    NQueensTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            VictoryPanel(
                time = "00:42",
                isNewRecord = true,
                onPlayAgainClick = {},
                onBackClick = {},
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VictoryPanelNoRecordPreview() {
    NQueensTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            VictoryPanel(
                time = "12:08",
                isNewRecord = false,
                onPlayAgainClick = {},
                onBackClick = {},
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
