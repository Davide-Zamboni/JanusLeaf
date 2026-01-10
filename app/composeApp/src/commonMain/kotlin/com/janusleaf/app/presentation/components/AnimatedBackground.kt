package com.janusleaf.app.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.janusleaf.app.presentation.theme.DuskPurple
import com.janusleaf.app.presentation.theme.LeafGreen
import com.janusleaf.app.presentation.theme.MossGreen
import com.janusleaf.app.presentation.theme.SageGreen
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated background with organic flowing shapes.
 * Creates a calming, nature-inspired atmosphere perfect for a journaling app.
 */
@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )
    
    val backgroundColor = MaterialTheme.colorScheme.background
    
    Box(modifier = modifier.fillMaxSize()) {
        // Base gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            LeafGreen.copy(alpha = 0.1f),
                            backgroundColor
                        )
                    )
                )
        )
        
        // Animated organic shapes
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOrganicShape(
                phase = phase1,
                color = SageGreen.copy(alpha = 0.08f),
                offsetY = 0.1f,
                amplitude = 80f,
                wavelength = 1.5f
            )
            
            drawOrganicShape(
                phase = phase2,
                color = MossGreen.copy(alpha = 0.06f),
                offsetY = 0.4f,
                amplitude = 100f,
                wavelength = 2f
            )
            
            drawOrganicShape(
                phase = phase3,
                color = DuskPurple.copy(alpha = 0.04f),
                offsetY = 0.7f,
                amplitude = 60f,
                wavelength = 1.2f
            )
        }
        
        // Content
        content()
    }
}

private fun DrawScope.drawOrganicShape(
    phase: Float,
    color: Color,
    offsetY: Float,
    amplitude: Float,
    wavelength: Float
) {
    val path = Path()
    val baseY = size.height * offsetY
    
    path.moveTo(0f, baseY)
    
    for (x in 0..size.width.toInt() step 4) {
        val normalizedX = x / size.width
        val y = baseY + sin((normalizedX * wavelength * PI + phase).toFloat()) * amplitude
        path.lineTo(x.toFloat(), y)
    }
    
    path.lineTo(size.width, size.height)
    path.lineTo(0f, size.height)
    path.close()
    
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color,
                color.copy(alpha = color.alpha * 0.5f),
                Color.Transparent
            ),
            startY = baseY - amplitude,
            endY = size.height
        )
    )
}

/**
 * Gradient orb decoration for visual interest.
 */
@Composable
fun GradientOrb(
    modifier: Modifier = Modifier,
    color: Color = SageGreen,
    size: Float = 200f
) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.3f),
                    color.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = center,
                radius = size
            ),
            radius = size,
            center = center
        )
    }
}
