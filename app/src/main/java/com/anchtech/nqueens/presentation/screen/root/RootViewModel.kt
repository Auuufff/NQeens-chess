package com.anchtech.nqueens.presentation.screen.root

import com.anchtech.nqueens.domain.repository.SettingsRepository
import com.anchtech.nqueens.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The theme `MainActivity` applies around the nav graph.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : BaseViewModel() {

    /**
     * `null` until the store has been read, and while unset.
     */
    val darkTheme: StateFlow<Boolean?> = settingsRepository.darkTheme
        .stateIn(scope = this, started = SharingStarted.Eagerly, initialValue = null)
}
