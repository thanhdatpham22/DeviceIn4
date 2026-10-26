package com.devicein4.app

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Decorative only: no CPU sampling, network requests, or animation in the background. */
@Composable
internal fun CpuPlayground(active: Boolean, dark: Boolean) {
    val owner = LocalLifecycleOwner.current
    var phase by remember { mutableFloatStateOf(0f) }
    val animate = active && (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled())
    LaunchedEffect(animate, owner) {
        if (!animate) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var start = 0L
            while (isActive) withFrameNanos { now ->
                if (start == 0L) start = now
                phase = ((now - start) % 8_000_000_000L) / 8_000_000_000f
            }
        }
    }
    val description = stringResource(R.string.cpu_play_description)
    Canvas(Modifier.fillMaxWidth().height(106.dp).clip(RoundedCornerShape(18.dp))
        .semantics { contentDescription = description }) {
        val sky = if (dark) Color(0xFF102C38) else Color(0xFFE3F7F1)
        val grass = if (dark) Color(0xFF244C38) else Color(0xFF8BCB8A)
        val mint = Color(0xFF5FE1D5)
        val ink = Color(0xFF12323A)
        val time = phase * (PI * 2).toFloat()
        val ground = size.height * .78f
        drawRect(Brush.verticalGradient(listOf(sky, sky.copy(alpha = .65f))))
        drawCircle(Color(0xFFFFD78A), 9.dp.toPx(), Offset(size.width * .86f, 23.dp.toPx()))
        repeat(3) { cloud ->
            val x = size.width * ((cloud * .37f + phase * .13f) % 1.2f) - 10.dp.toPx()
            val y = (17 + cloud * 9).dp.toPx()
            drawRoundRect(Color.White.copy(alpha = if (dark) .13f else .65f), Offset(x, y),
                Size(32.dp.toPx(), 7.dp.toPx()), CornerRadius(5.dp.toPx()))
        }
        val hill = Path().apply {
            moveTo(0f, ground)
            quadraticTo(size.width * .3f, ground - 15.dp.toPx(), size.width * .65f, ground)
            quadraticTo(size.width * .85f, ground - 10.dp.toPx(), size.width, ground)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(hill, grass.copy(alpha = .6f))
        drawRect(grass, Offset(0f, ground), Size(size.width, size.height - ground))
        repeat(40) { blade ->
            val x = size.width * blade / 39f
            val sway = sin(time * 2f + blade).toFloat() * 1.5.dp.toPx()
            val length = (3 + blade % 4).dp.toPx()
            drawLine(Color(0xFF6AAF72), Offset(x, ground + 5.dp.toPx()), Offset(x + sway, ground - length), 1.2.dp.toPx())
            if (blade % 7 == 2) drawCircle(Color(0xFFFFE3A2), 1.6.dp.toPx(), Offset(x + sway, ground - length - 1.dp.toPx()))
        }
        val travel = if (phase < .5f) phase * 2f else (1f - phase) * 2f
        val centerX = size.width * (.15f + travel * .7f)
        val hop = abs(sin(time * 8f)).toFloat() * 10.dp.toPx()
        val stride = sin(time * 16f).toFloat()
        drawOval(Color.Black.copy(alpha = .18f), Offset(centerX - 17.dp.toPx(), ground - 2.dp.toPx()),
            Size(34.dp.toPx(), 5.dp.toPx()))
        translate(centerX, ground - 27.dp.toPx() - hop) {
            scale(if (phase < .5f) 1f else -1f, 1f, Offset.Zero) {
                rotate(stride * 4f, Offset.Zero) {
                    val w = 29.dp.toPx(); val h = 27.dp.toPx()
                    repeat(5) { pin ->
                        val p = -10.dp.toPx() + pin * 5.dp.toPx()
                        drawLine(Color(0xFFB4C9CB), Offset(-w / 2 - 4.dp.toPx(), p), Offset(-w / 2, p), 2.dp.toPx())
                        drawLine(Color(0xFFB4C9CB), Offset(w / 2, p), Offset(w / 2 + 4.dp.toPx(), p), 2.dp.toPx())
                        drawLine(Color(0xFFB4C9CB), Offset(p, -h / 2 - 4.dp.toPx()), Offset(p, -h / 2), 2.dp.toPx())
                    }
                    for (side in listOf(-1, 1)) {
                        val step = stride * side * 6.dp.toPx()
                        drawLine(mint, Offset(side * 7.dp.toPx(), h / 2),
                            Offset(side * 7.dp.toPx() + step, h / 2 + 10.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                        drawLine(mint, Offset(side * 14.dp.toPx(), 2.dp.toPx()),
                            Offset(side * 23.dp.toPx(), step), 2.dp.toPx(), StrokeCap.Round)
                        drawCircle(Color(0xFFFFD78A), 2.5.dp.toPx(), Offset(side * 23.dp.toPx(), step))
                    }
                    drawRoundRect(mint, Offset(-w / 2, -h / 2), Size(w, h), CornerRadius(5.dp.toPx()))
                    drawRoundRect(ink, Offset(-w / 2 + 3.dp.toPx(), -h / 2 + 3.dp.toPx()),
                        Size(w - 6.dp.toPx(), h - 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(-5.dp.toPx(), -2.dp.toPx()))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(5.dp.toPx(), -2.dp.toPx()))
                    val smile = Path().apply { moveTo(-4.dp.toPx(), 4.dp.toPx()); quadraticTo(0f, 9.dp.toPx(), 5.dp.toPx(), 3.dp.toPx()) }
                    drawPath(smile, mint, style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}
