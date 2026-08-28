package com.anchtech.nqueens.presentation.component

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 6f

/**
 * Makes its content pinch-zoomable and pannable, seen through a viewport of whatever size
 * this is given. The content sits in the middle of the viewport and may grow past it on
 * either axis; panning is clamped so it can never be dragged in off an edge.
 */
@Composable
fun Zoomable(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)

                    val anchor = centroid - Offset(size.width / 2f, size.height / 2f)
                    val moved = anchor + (offset - anchor) * (next / scale) + pan

                    val slackX = max(0f, (contentSize.width * next - size.width) / 2f)
                    val slackY = max(0f, (contentSize.height * next - size.height) / 2f)

                    scale = next
                    offset = Offset(
                        x = moved.x.coerceIn(-slackX, slackX),
                        y = moved.y.coerceIn(-slackY, slackY),
                    )
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            content()
        }
    }
}
