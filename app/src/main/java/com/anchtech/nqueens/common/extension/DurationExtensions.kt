package com.anchtech.nqueens.common.extension

import kotlin.time.Duration

/**
 * Formats as `mm:ss`. Minutes are not wrapped at an hour: a long solve reads `72:14`.
 */
fun Duration.formatAsClock(): String {
    val totalSeconds = inWholeSeconds
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
