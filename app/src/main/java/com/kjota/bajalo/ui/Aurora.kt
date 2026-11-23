package com.kjota.bajalo.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colores de la identidad visual (aurora). */
object Glass {
    val Violet = Color(0xFF8B7BFF)
    val Teal = Color(0xFF19D3AE)
    val Pink = Color(0xFFFF5FA2)
    val Blue = Color(0xFF3B82F6)
    val Soft = Color(0xB3FFFFFF)
}

/**
 * Fondo "aurora": una malla de degradados radiales (violeta, turquesa, rosa y azul)
 * que se mueve muy despacio sobre un fondo casi negro.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aurora-t",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B14))
            .drawBehind {
                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
                blob(Color(0x886C4DFF), w * (0.15f + 0.20f * t), h * (0.08f + 0.06f * t), w * 0.95f)
                blob(Color(0x7719D3AE), w * (0.95f - 0.25f * t), h * (0.28f + 0.10f * t), w * 0.85f)
                blob(Color(0x66FF5FA2), w * (0.20f + 0.15f * t), h * (0.62f - 0.08f * t), w * 0.80f)
                blob(Color(0x663B82F6), w * (0.90f - 0.10f * t), h * (0.90f - 0.12f * t), w * 0.90f)
            },
        content = content,
    )
}

/**
 * Tarjeta de vidrio: relleno translúcido con un leve degradado y un borde de luz.
 * (Compose no desenfoca lo que hay detrás de forma nativa en todos los teléfonos; como el fondo
 * ya es suave, translucidez + borde de luz dan el mismo efecto de vidrio esmerilado.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    var m = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(tint.copy(alpha = 0.18f), tint.copy(alpha = 0.06f)),
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.08f)),
            ),
            shape = shape,
        )
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(modifier = m.padding(16.dp), content = content)
}
