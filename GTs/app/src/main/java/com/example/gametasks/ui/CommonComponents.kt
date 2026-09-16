package com.example.gametasks.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedText(
    text: String,
    isReduceMotion: Boolean,
    color: Color,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    if (isReduceMotion) {
        Text(text, color = color, fontWeight = fontWeight, fontSize = fontSize, modifier = modifier)
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "text")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Text(
            text = text,
            color = color.copy(alpha = alpha),
            fontWeight = fontWeight,
            fontSize = fontSize,
            modifier = modifier.graphicsLayer {
                this.alpha = alpha
            }
        )
    }
}

@Composable
fun FutureButton(
    onClick: () -> Unit,
    isReduceMotion: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = if (isReduceMotion) snap() else spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 12.dp,
        animationSpec = if (isReduceMotion) snap() else tween(150),
        label = "elevation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "btnGlow")
    val glowAlphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )
    val glowAlpha = if (isReduceMotion) 1f else glowAlphaAnim

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (isReduceMotion) 0.5f else glowAlpha * 0.5f)),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
