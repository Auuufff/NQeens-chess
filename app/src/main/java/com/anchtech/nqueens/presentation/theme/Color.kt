package com.anchtech.nqueens.presentation.theme

import androidx.compose.ui.graphics.Color

/*
 * Palette: "Sage & Parchment".
 *
 * A muted forest green carries the UI, a warm bronze provides the accent, and the board
 * itself sits in parchment and sage — quiet enough that the queens and the conflict
 * markers are the only things competing for attention.
 *
 * Raw tokens only. Semantic assignment happens in Theme.kt and BoardColors.kt.
 */

// ---- Forest (primary) -------------------------------------------------------------
internal val Forest10 = Color(0xFF00210F)
internal val Forest20 = Color(0xFF00391D)
internal val Forest30 = Color(0xFF00522C)
internal val Forest40 = Color(0xFF2E6B45)
internal val Forest80 = Color(0xFF9BD5AF)
internal val Forest90 = Color(0xFFB7F1C9)

// ---- Sage (secondary) -------------------------------------------------------------
internal val Sage10 = Color(0xFF0D1F13)
internal val Sage20 = Color(0xFF223526)
internal val Sage30 = Color(0xFF384B3B)
internal val Sage40 = Color(0xFF4F6353)
internal val Sage80 = Color(0xFFB6CCB9)
internal val Sage90 = Color(0xFFD1E8D4)

// ---- Bronze (tertiary / accent) ---------------------------------------------------
internal val Bronze10 = Color(0xFF291800)
internal val Bronze20 = Color(0xFF442B00)
internal val Bronze30 = Color(0xFF614000)
internal val Bronze40 = Color(0xFF7F5600)
internal val Bronze80 = Color(0xFFF2BE48)
internal val Bronze90 = Color(0xFFFFDEA6)

// ---- Neutrals ---------------------------------------------------------------------
internal val Ink10 = Color(0xFF10140F)
internal val Ink20 = Color(0xFF1A1F18)
internal val Ink90 = Color(0xFFE1E4DD)
internal val Ink95 = Color(0xFFEFF2EB)

internal val Paper = Color(0xFFFBFAF4)
internal val PaperDim = Color(0xFFDBDBD4)
internal val PaperBright = Color(0xFFFBFAF4)

internal val NeutralVariant30 = Color(0xFF414942)
internal val NeutralVariant50 = Color(0xFF717972)
internal val NeutralVariant60 = Color(0xFF8B938B)
internal val NeutralVariant80 = Color(0xFFC1C9C0)
internal val NeutralVariant90 = Color(0xFFDDE5DB)

// ---- Error ------------------------------------------------------------------------
internal val Error10 = Color(0xFF410002)
internal val Error20 = Color(0xFF690005)
internal val Error30 = Color(0xFF93000A)
internal val Error40 = Color(0xFFBA1A1A)
internal val Error80 = Color(0xFFFFB4AB)
internal val Error90 = Color(0xFFFFDAD6)

// ---- Board ------------------------------------------------------------------------
internal val ParchmentSquare = Color(0xFFF0E7D2)
internal val SageSquare = Color(0xFF55704D)
internal val ParchmentSquareDark = Color(0xFFB6AB92)
internal val SageSquareDark = Color(0xFF4E6949)

internal val QueenInk = Color(0xFF23301E)
internal val QueenIvory = Color(0xFFF6F2E6)

/* A conflicting square is repainted, not tinted — the cell itself turns red. */
internal val ConflictParchment = Color(0xFFE4B0A6)
internal val ConflictSage = Color(0xFF8A5347)
internal val ConflictParchmentDark = Color(0xFFB08A7C)
internal val ConflictSageDark = Color(0xFF6B4239)

internal val VictoryGold = Color(0xFFE8B44C)
