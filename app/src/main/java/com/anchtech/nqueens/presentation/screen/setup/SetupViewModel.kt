package com.anchtech.nqueens.presentation.screen.setup

import com.anchtech.nqueens.common.extension.formatAsClock
import com.anchtech.nqueens.domain.repository.SettingsRepository
import com.anchtech.nqueens.presentation.base.BaseComposeViewModel
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : BaseComposeViewModel<SetupState, SetupAction>(SetupState()) {

    init {
        updateState {
            it.copy(
                onSizeSelected = ::handleSizeSelected,
                onStartClick = ::handleStartClick,
                onDarkThemeChange = ::handleDarkThemeChange,
            )
        }
        observeBestTimes()
        observeDarkTheme()
    }

    private fun handleSizeSelected(size: Int) = updateState { it.copy(selectedSize = size) }

    private fun handleStartClick() = sendAction(SetupAction.StartGame(state.value.selectedSize))

    private fun handleDarkThemeChange(enabled: Boolean) = launch { settingsRepository.setDarkTheme(enabled) }

    /**
     * Collected, not read once, so a time set this session appears on return.
     */
    private fun observeBestTimes() = launch {
        settingsRepository.bestTimes.collect { times ->
            val records = times.entries
                .sortedBy { it.key }
                .take(MAX_RECORDS)
                .map { (size, time) -> UiBestTime(size = size, time = time.formatAsClock()) }
            updateState { it.copy(records = records) }
        }
    }

    private fun observeDarkTheme() = launch {
        settingsRepository.darkTheme.collect { darkTheme -> updateState { it.copy(darkTheme = darkTheme) } }
    }

    private companion object {
        /**
         * The record card renders into a plain Column, so the list it is given has to be
         * bounded rather than lazy.
         */
        const val MAX_RECORDS = 30
    }
}
