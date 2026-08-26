package com.anchtech.nqueens.presentation.base

/**
 * Marker for a one-time, transient effect emitted by a ViewModel: a sound, a haptic,
 * a navigation request.
 *
 * Anything the user could still be looking at a second later belongs in [BaseState]
 * instead — actions are dropped while the screen is off-composition and are never
 * replayed.
 */
interface BaseAction
