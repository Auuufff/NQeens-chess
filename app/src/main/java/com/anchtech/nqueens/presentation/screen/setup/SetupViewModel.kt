package com.anchtech.nqueens.presentation.screen.setup

import com.anchtech.nqueens.common.extension.formatAsClock
import com.anchtech.nqueens.domain.repository.BestTimesRepository
import com.anchtech.nqueens.presentation.base.BaseComposeViewModel
import com.anchtech.nqueens.presentation.screen.setup.model.UiBestTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val bestTimesRepository: BestTimesRepository,
) : BaseComposeViewModel<SetupState, SetupAction>(SetupState()) {

    // todo describe in readme why its not very good place to observe
    init {
        updateState {
            it.copy(
                onSizeSelected = ::handleSizeSelected,
                onStartClick = ::handleStartClick,
            )
        }
        observeBestTimes()
    }

    private fun handleSizeSelected(size: Int) = updateState { it.copy(selectedSize = size) }

    private fun handleStartClick() = sendAction(SetupAction.StartGame(state.value.selectedSize))

    /**
     * Collected, not read once, so a time set this session appears on return.
     */
    private fun observeBestTimes() = launch {
        bestTimesRepository.bestTimes.collect { times ->
            val records = times.entries
                .sortedBy { it.key }
                .take(MAX_RECORDS)
                .map { (size, time) -> UiBestTime(size = size, time = time.formatAsClock()) }
            updateState { it.copy(records = records) }
        }
    }

    private companion object {
        /**
         * The record card renders into a plain Column, so the list it is given has to be
         * bounded rather than lazy.
         */
        const val MAX_RECORDS = 30
    }
}
