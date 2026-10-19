package com.weatherfocus.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weatherfocus.app.data.model.ConditionGroup
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Glossy "3D toy" style weather icons - soft shaded spheres/clouds with a specular highlight and a
 * gentle drop shadow underneath, in the spirit of premium 3D emoji packs, drawn live on a Canvas
 * (no external image/gif assets needed) and lightly animated so they feel alive.
 */
@Composable
fun PremiumWeatherIcon(group: ConditionGroup, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "weatherIcon3d")
    // 0f -> 1f, loops forever - drives spin, falling drops/flakes, drifting fog, lightning flicker.
    val loop by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "loop"
    )
    // -1f <-> 1f, eases back and forth - drives the gentle bob/breathe.
    val bob by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "bob"
    )

    // Pixar-style "pop in": icon bounces onto screen with a springy overshoot instead of just appearing,
    // the same trick character rigs use to feel alive rather than static.
    val entrance = remember(group) { Animatable(0f) }
    LaunchedEffect(group) {
        entrance.snapTo(0f)
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }
    // Continuous gentle "breathing" scale on top of the entrance pop, so the icon never sits perfectly still.
    val breathe = 1f + bob * 0.035f

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val s = entrance.value * breathe
                scaleX = s
                scaleY = s
            }
    ) {
        drawGroundShadow(bob)
        when (group) {
            ConditionGroup.CLEAR -> drawSun3D(loop, bob)
            ConditionGroup.CLOUDY -> drawCloud3D(bob, light = Color(0xFFFDFEFF), mid = Color(0xFFD7DEE8), dark = Color(0xFFA6B2C2))
            ConditionGroup.RAIN -> {
                drawCloud3D(bob, light = Color(0xFFF3F6FA), mid = Color(0xFFC3CCD8), dark = Color(0xFF8B96A6))
                drawRainDrops3D(loop)
            }
            ConditionGroup.SNOW -> {
                drawCloud3D(bob, light = Color(0xFFFAFCFF), mid = Color(0xFFDCE6F0), dark = Color(0xFFAEC0D4))
                drawSnowflakes3D(loop)
            }
            ConditionGroup.THUNDER -> {
                drawCloud3D(bob, light = Color(0xFF8B8299), mid = Color(0xFF564E6E), dark = Color(0xFF322B47))
                drawBolt3D(loop)
            }
            ConditionGroup.FOG -> drawFog3D(loop)
            ConditionGroup.UNKNOWN -> drawCloud3D(bob, light = Color(0xFFF1F2F4), mid = Color(0xFFCBCFD6), dark = Color(0xFF9AA1AC))
        }
    }
}

/**
 * A soft, layered shadow ellipse grounding the icon - stacking a few translucent ovals of shrinking size
 * fakes a blurred contact shadow (Canvas has no cheap blur), which reads as much more "3D toy sitting on
 * a surface" than one flat oval. It also breathes subtly opposite the icon's bob, like real contact shadows do.
 */
private fun DrawScope.drawGroundShadow(bob: Float = 0f) {
    val w = size.width
    val h = size.height
    val spread = 1f - abs(bob) * 0.06f
    val layers = listOf(0.05f to 1.35f, 0.09f to 1.05f, 0.16f to 0.75f)
    layers.forEach { (alpha, scale) ->
        val ow = w * 0.56f * scale * spread
        drawOval(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(w * 0.5f - ow / 2f, h * 0.89f),
            size = Size(ow, h * 0.095f * scale)
        )
    }
}

/** A tiny extra glint - a small crisp dot near the main specular highlight - for that extra glossy "toy" polish. */
private fun DrawScope.drawSparkle(center: Offset, r: Float) {
    drawCircle(color = Color.White.copy(alpha = 0.95f), radius = r, center = center)
}

/** A shared "glossy sphere" gradient: light source top-left, deepening to a rich shadow tone bottom-right. */
private fun sphereBrush(w: Float, h: Float, light: Color, mid: Color, dark: Color): Brush = Brush.radialGradient(
    colors = listOf(light, mid, dark),
    center = Offset(w * 0.38f, h * 0.34f),
    radius = w * 0.85f
)

private fun DrawScope.drawSpecularHighlight(center: Offset, r: Float) {
    drawOval(
        color = Color.White.copy(alpha = 0.75f),
        topLeft = Offset(center.x - r * 0.55f, center.y - r * 0.62f),
        size = Size(r * 0.5f, r * 0.32f)
    )
}

private fun DrawScope.drawSun3D(loop: Float, bob: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.46f
    val r = size.minDimension * 0.24f
    val center = Offset(cx, cy)

    // Warm glow that breathes.
    val glowR = r * (2.1f + 0.2f * bob)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFDD7A).copy(alpha = 0.5f), Color.Transparent),
            center = center,
            radius = glowR
        ),
        radius = glowR,
        center = center
    )

    // Rays: chunky rounded rays, slowly spinning, filled with a warm gradient for a glossy feel.
    val rayBrush = Brush.linearGradient(colors = listOf(Color(0xFFFFD24C), Color(0xFFFF9E2C)))
    rotate(degrees = loop * 360f, pivot = center) {
        for (i in 0 until 8) {
            rotate(degrees = i * 45f, pivot = center) {
                val path = Path().apply {
                    val rayLen = r * 0.62f
                    val rayW = r * 0.16f
                    moveTo(cx - rayW / 2f, cy - r * 1.18f)
                    lineTo(cx + rayW / 2f, cy - r * 1.18f)
                    lineTo(cx, cy - r * 1.18f - rayLen)
                    close()
                }
                drawPath(path, brush = rayBrush)
            }
        }
    }

    // Glossy sun sphere.
    drawCircle(
        brush = sphereBrush(size.width, size.height, Color(0xFFFFF3B0), Color(0xFFFFC547), Color(0xFFE8890A)),
        radius = r,
        center = center
    )
    drawSpecularHighlight(center, r)
    drawSparkle(Offset(center.x - r * 0.32f, center.y - r * 0.4f), r * 0.06f)
}

/**
 * Two puffy, glossy clouds overlapping - a smaller, darker/duller one peeking out top-left behind, and
 * a larger, brighter main one in front, each a different rounded shape, so the icon reads as "two clouds"
 * rather than one blob, shaded like a soft 3D toy.
 */
private fun DrawScope.drawCloud3D(bob: Float, light: Color, mid: Color, dark: Color) {
    val w = size.width
    val h = size.height
    val dx = bob * w * 0.03f

    // Back cloud: smaller, shifted up-left, slightly duller/darker so it reads as "behind".
    val backLight = mid
    val backMid = dark
    val backDark = Color(dark.red * 0.82f, dark.green * 0.82f, dark.blue * 0.82f, dark.alpha)
    val backBrush = sphereBrush(w, h, backLight, backMid, backDark)
    drawRoundRect(
        brush = backBrush,
        topLeft = Offset(w * 0.06f - dx, h * 0.26f),
        size = Size(w * 0.46f, h * 0.24f),
        cornerRadius = CornerRadius(h * 0.12f)
    )
    drawCircle(brush = backBrush, radius = w * 0.15f, center = Offset(w * 0.16f - dx, h * 0.32f))
    drawCircle(brush = backBrush, radius = w * 0.18f, center = Offset(w * 0.34f - dx, h * 0.26f))

    // Front cloud: larger, brighter, main focal shape - different silhouette (wider, lower, three lobes).
    val frontBrush = sphereBrush(w, h, light, mid, dark)
    drawRoundRect(
        brush = frontBrush,
        topLeft = Offset(w * 0.18f + dx, h * 0.46f),
        size = Size(w * 0.70f, h * 0.32f),
        cornerRadius = CornerRadius(h * 0.16f)
    )
    drawCircle(brush = frontBrush, radius = w * 0.20f, center = Offset(w * 0.33f + dx, h * 0.49f))
    drawCircle(brush = frontBrush, radius = w * 0.27f, center = Offset(w * 0.55f + dx, h * 0.42f))
    drawCircle(brush = frontBrush, radius = w * 0.19f, center = Offset(w * 0.73f + dx, h * 0.51f))

    drawSpecularHighlight(Offset(w * 0.44f + dx, h * 0.44f), w * 0.30f)
    drawSparkle(Offset(w * 0.62f + dx, h * 0.36f), w * 0.035f)
}

private fun DrawScope.drawRainDrops3D(loop: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until 3) {
        val phase = (loop + i * 0.33f) % 1f
        val y0 = h * 0.72f
        val y1 = h * 0.98f
        val y = y0 + (y1 - y0) * phase
        val alpha = (1f - phase).coerceIn(0.15f, 1f)
        val x = w * (0.32f + i * 0.19f)
        val dropW = w * 0.075f
        val path = Path().apply {
            moveTo(x, y - dropW * 0.9f)
            quadraticBezierTo(x + dropW * 0.55f, y - dropW * 0.15f, x, y + dropW * 0.55f)
            quadraticBezierTo(x - dropW * 0.55f, y - dropW * 0.15f, x, y - dropW * 0.9f)
            close()
        }
        drawPath(
            path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFAEE0FF).copy(alpha = alpha), Color(0xFF3E8FD6).copy(alpha = alpha))
            )
        )
    }
}

private fun DrawScope.drawSnowflakes3D(loop: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until 3) {
        val phase = (loop + i * 0.33f) % 1f
        val y = h * 0.70f + h * 0.26f * phase
        val sway = sin((phase * 2 * PI) + i).toFloat() * w * 0.05f
        val x = w * (0.32f + i * 0.19f) + sway
        val alpha = (1f - phase * 0.7f).coerceIn(0.2f, 1f)
        val r = w * 0.045f
        // Six-point sparkle flake.
        for (a in 0 until 3) {
            val angle = Math.toRadians((a * 60).toDouble())
            val dx = (cos(angle) * r).toFloat()
            val dy = (sin(angle) * r).toFloat()
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(x - dx, y - dy),
                end = Offset(x + dx, y + dy),
                strokeWidth = w * 0.018f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = Color.White.copy(alpha = alpha), radius = r * 0.28f, center = Offset(x, y))
    }
}

private fun DrawScope.drawBolt3D(loop: Float) {
    val w = size.width
    val h = size.height
    val flicker = 0.55f + 0.45f * abs(sin(loop * 2 * PI * 1.6)).toFloat()
    val path = Path().apply {
        moveTo(w * 0.57f, h * 0.58f)
        lineTo(w * 0.43f, h * 0.80f)
        lineTo(w * 0.51f, h * 0.80f)
        lineTo(w * 0.41f, h * 1.02f)
        lineTo(w * 0.61f, h * 0.75f)
        lineTo(w * 0.52f, h * 0.75f)
        close()
    }
    // Soft outer glow: same path, larger, low alpha.
    scale(1.4f, pivot = Offset(w * 0.5f, h * 0.8f)) {
        drawPath(path, color = Color(0xFFFFE45E).copy(alpha = flicker * 0.25f))
    }
    drawPath(
        path,
        brush = Brush.linearGradient(colors = listOf(Color(0xFFFFF3A0).copy(alpha = flicker), Color(0xFFFFB020).copy(alpha = flicker)))
    )
}

/** Which small glossy 3D stat icon to draw for the "mini stat" rows (rain %, wind, UV, sunrise, sunset, humidity, pressure). */
enum class StatIconType { RAIN, WIND, UV, SUNRISE, SUNSET, HUMIDITY, PRESSURE }

/**
 * A small glossy 3D-toy style icon for the mini weather stats, drawn in the same Canvas/gradient/highlight
 * language as [PremiumWeatherIcon] so every icon in the app (big condition icons and small stat icons alike)
 * shares one consistent, colourful, animated style instead of mixing in flat outline icons.
 */
@Composable
fun StatIcon3D(type: StatIconType, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "statIcon3d")
    val loop by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "statLoop"
    )
    val bob by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "statBob"
    )

    Canvas(modifier = modifier.size(size)) {
        when (type) {
            StatIconType.RAIN -> drawStatRainDrop(bob)
            StatIconType.WIND -> drawStatWind(loop, bob)
            StatIconType.UV -> drawStatUv(loop)
            StatIconType.SUNRISE -> drawStatSunriseSunset(loop, rising = true)
            StatIconType.SUNSET -> drawStatSunriseSunset(loop, rising = false)
            StatIconType.HUMIDITY -> drawStatRainDrop(bob)
            StatIconType.PRESSURE -> drawStatGauge(bob)
        }
    }
}

/** Single glossy blue droplet, gently bobbing - used for rain chance / humidity. */
private fun DrawScope.drawStatRainDrop(bob: Float) {
    val w = size.width
    val h = size.height
    val cy = h * 0.5f + bob * h * 0.03f
    val cx = w * 0.5f
    val r = w * 0.34f
    val path = Path().apply {
        moveTo(cx, cy - r * 1.15f)
        quadraticBezierTo(cx + r * 1.05f, cy - r * 0.05f, cx, cy + r * 0.95f)
        quadraticBezierTo(cx - r * 1.05f, cy - r * 0.05f, cx, cy - r * 1.15f)
        close()
    }
    drawPath(
        path,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFAEE0FF), Color(0xFF3E8FD6), Color(0xFF1F5FA0)),
            center = Offset(cx - r * 0.25f, cy - r * 0.2f),
            radius = r * 1.7f
        )
    )
    drawOval(
        color = Color.White.copy(alpha = 0.7f),
        topLeft = Offset(cx - r * 0.32f, cy - r * 0.55f),
        size = Size(r * 0.4f, r * 0.28f)
    )
}

/** Glossy grey-blue swirl with motion streaks - used for wind speed. */
private fun DrawScope.drawStatWind(loop: Float, bob: Float) {
    val w = size.width
    val h = size.height
    val cy = h * 0.5f + bob * h * 0.02f
    val brush = Brush.linearGradient(colors = listOf(Color(0xFFAEC2D6), Color(0xFF5D7C9A)))
    val lengths = listOf(0.62f, 0.46f, 0.36f)
    val yOffsets = listOf(-0.16f, 0f, 0.16f)
    lengths.forEachIndexed { i, len ->
        val phase = ((loop + i * 0.2f) % 1f)
        val wobble = sin(phase * 2 * PI).toFloat() * w * 0.02f
        val y = cy + h * yOffsets[i]
        val startX = w * 0.16f + wobble
        val endX = startX + w * len
        val path = Path().apply {
            moveTo(startX, y)
            quadraticBezierTo((startX + endX) / 2f, y - h * 0.08f, endX, y)
            quadraticBezierTo(endX + w * 0.05f, y + h * 0.05f, endX + w * 0.09f, y - h * 0.02f)
        }
        drawPath(path, brush = brush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f, cap = StrokeCap.Round))
    }
}

/** Glossy amber sun-disc with a bold "UV" ring of rays - used for UV index. */
private fun DrawScope.drawStatUv(loop: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = w * 0.26f
    val center = Offset(cx, cy)
    rotate(degrees = loop * 360f, pivot = center) {
        for (i in 0 until 8) {
            rotate(degrees = i * 45f, pivot = center) {
                drawLine(
                    brush = Brush.linearGradient(colors = listOf(Color(0xFFFFD24C), Color(0xFFFF7A3D))),
                    start = Offset(cx, cy - r * 1.25f),
                    end = Offset(cx, cy - r * 1.7f),
                    strokeWidth = w * 0.09f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF3B0), Color(0xFFFFC547), Color(0xFFE8890A)),
            center = Offset(cx - r * 0.35f, cy - r * 0.35f),
            radius = r * 1.6f
        ),
        radius = r,
        center = center
    )
    drawOval(
        color = Color.White.copy(alpha = 0.75f),
        topLeft = Offset(cx - r * 0.5f, cy - r * 0.55f),
        size = Size(r * 0.45f, r * 0.28f)
    )
}

/** Glossy horizon arc with a half sun rising or setting through it, and small rays - sunrise/sunset. */
private fun DrawScope.drawStatSunriseSunset(loop: Float, rising: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val horizonY = h * 0.62f
    val bob = (sin(loop * 2 * PI).toFloat() + 1f) / 2f // 0..1 gentle pulse
    val sunY = horizonY - h * (0.12f + 0.05f * bob) * (if (rising) 1f else 1f)
    val r = w * 0.24f

    // Rays fanning from the sun, above the horizon only.
    val rayBrush = Brush.linearGradient(colors = listOf(Color(0xFFFFD24C), Color(0xFFFF9E2C)))
    for (i in -2..2) {
        val angle = Math.toRadians((90.0 + i * 22))
        val x1 = cx + (cos(angle) * r * 1.3).toFloat()
        val y1 = sunY - (sin(angle) * r * 1.3).toFloat()
        val x2 = cx + (cos(angle) * r * 1.9).toFloat()
        val y2 = sunY - (sin(angle) * r * 1.9).toFloat()
        if (y2 < horizonY + h * 0.05f) {
            drawLine(brush = rayBrush, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
        }
    }

    // Sun disc, clipped to above the horizon line for a rising/setting look.
    clipRect(left = 0f, top = 0f, right = w, bottom = horizonY) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE9A0), Color(0xFFFFB648), Color(0xFFE8890A)),
                center = Offset(cx - r * 0.3f, sunY - r * 0.3f),
                radius = r * 1.6f
            ),
            radius = r,
            center = Offset(cx, sunY)
        )
    }

    // Glossy horizon bar.
    drawRoundRect(
        brush = Brush.horizontalGradient(colors = listOf(Color(0xFF6D5AA6), Color(0xFFE07A5F))),
        topLeft = Offset(w * 0.08f, horizonY),
        size = Size(w * 0.84f, h * 0.08f),
        cornerRadius = CornerRadius(h * 0.04f)
    )

    // Small direction arrow to disambiguate rising vs setting.
    val arrowY = horizonY + h * 0.2f
    val arrowPath = Path().apply {
        if (rising) {
            moveTo(cx - w * 0.09f, arrowY + h * 0.05f)
            lineTo(cx, arrowY - h * 0.06f)
            lineTo(cx + w * 0.09f, arrowY + h * 0.05f)
        } else {
            moveTo(cx - w * 0.09f, arrowY - h * 0.06f)
            lineTo(cx, arrowY + h * 0.05f)
            lineTo(cx + w * 0.09f, arrowY - h * 0.06f)
        }
    }
    drawPath(arrowPath, color = Color(0xFFE07A5F), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f, cap = StrokeCap.Round))
}

/** Glossy dial/gauge - used for pressure. */
private fun DrawScope.drawStatGauge(bob: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.54f
    val r = w * 0.32f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFEDEDEF), Color(0xFFB8BCC4), Color(0xFF7C818C)),
            center = Offset(cx - r * 0.3f, cy - r * 0.3f),
            radius = r * 1.6f
        ),
        radius = r,
        center = Offset(cx, cy)
    )
    val needleAngle = Math.toRadians((200 + bob * 40).toDouble())
    drawLine(
        color = Color(0xFFE8582C),
        start = Offset(cx, cy),
        end = Offset(cx + (cos(needleAngle) * r * 0.7).toFloat(), cy + (sin(needleAngle) * r * 0.7).toFloat()),
        strokeWidth = w * 0.06f,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFF3D3F44), radius = r * 0.12f, center = Offset(cx, cy))
    drawOval(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = Offset(cx - r * 0.5f, cy - r * 0.6f),
        size = Size(r * 0.4f, r * 0.24f)
    )
}

private fun DrawScope.drawFog3D(loop: Float) {
    val w = size.width
    val h = size.height
    val tones = listOf(
        Color(0xFFDCE3EA) to Color(0xFFAAB6C4),
        Color(0xFFCBD5DF) to Color(0xFF95A3B4),
        Color(0xFFB9C6D2) to Color(0xFF7F8FA2)
    )
    for (i in 0 until 3) {
        val yy = h * (0.36f + i * 0.19f)
        val amp = w * 0.07f
        val xOff = sin((loop * 2 * PI).toFloat() + i * 1.4f) * amp
        val (light, dark) = tones[i]
        drawRoundRect(
            brush = Brush.horizontalGradient(colors = listOf(light, dark, light)),
            topLeft = Offset(w * 0.14f + xOff, yy),
            size = Size(w * 0.66f, h * 0.11f),
            cornerRadius = CornerRadius(h * 0.055f),
            alpha = 0.9f - i * 0.08f
        )
    }
}
