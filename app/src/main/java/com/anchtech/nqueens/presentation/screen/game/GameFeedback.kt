package com.anchtech.nqueens.presentation.screen.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.anchtech.nqueens.R

/**
 * A [GameFeedback] released when the caller leaves the composition.
 */
@Composable
internal fun rememberGameFeedback(): GameFeedback {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    return remember(context, haptics) { GameFeedback(context, haptics) }
}

/**
 * Haptic and sound feedback for board events.
 *
 * Owns a [SoundPool] freed through [RememberObserver], so it must be held by [remember];
 * once freed the instance is silent.
 */
internal class GameFeedback(
    context: Context,
    private val haptics: HapticFeedback,
) : RememberObserver {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds: Map<Int, Int> = SOUNDS.associateWith { soundPool.load(context, it, LOAD_PRIORITY) }

    /**
     * A queen placed on a square no other queen attacks.
     */
    fun queenPlaced() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        play(R.raw.queen_place)
    }

    /**
     * A queen placed on an attacked square.
     */
    fun queenClashed() {
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        play(R.raw.queen_clash)
    }

    fun queenRemoved() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        play(R.raw.queen_remove)
    }

    fun solved() {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        play(R.raw.victory)
    }

    override fun onRemembered() = Unit

    /**
     * The composition holding this instance ended.
     */
    override fun onForgotten() = release()

    /**
     * The composition holding this instance was discarded before it was applied.
     */
    override fun onAbandoned() = release()

    private fun release() {
        soundPool.release()
    }

    private fun play(@RawRes sound: Int) {
        val soundId = soundIds[sound] ?: return
        soundPool.play(soundId, VOLUME, VOLUME, STREAM_PRIORITY, NO_LOOP, NORMAL_RATE)
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val LOAD_PRIORITY = 1
        const val STREAM_PRIORITY = 1
        const val NO_LOOP = 0
        const val NORMAL_RATE = 1f
        const val VOLUME = 0.6f

        val SOUNDS = listOf(R.raw.queen_place, R.raw.queen_clash, R.raw.queen_remove, R.raw.victory)
    }
}
