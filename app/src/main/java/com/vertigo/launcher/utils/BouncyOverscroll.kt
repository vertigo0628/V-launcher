package com.vertigo.launcher.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

@Composable
fun rememberBouncyOverscrollModifier(): Modifier {
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If currently overscrolling, eat the scroll to reduce it
                if (overscrollOffset.value != 0f) {
                    val sign = if (overscrollOffset.value > 0) 1 else -1
                    val delta = available.y
                    if (delta * sign < 0) {
                        // Scrolling back towards 0
                        val newOffset = overscrollOffset.value + delta
                        if ((newOffset * sign) < 0) {
                            scope.launch { overscrollOffset.snapTo(0f) }
                            return Offset(0f, overscrollOffset.value)
                        } else {
                            scope.launch { overscrollOffset.snapTo(newOffset) }
                            return Offset(0f, delta)
                        }
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y != 0f) {
                    val newOffset = overscrollOffset.value + (available.y * 0.3f) // Friction
                    scope.launch {
                        overscrollOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y) // consume rest
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (overscrollOffset.value != 0f || available.y != 0f) {
                    val target = overscrollOffset.value + (available.y * 0.1f)
                    val flingVelocity = available.y
                    
                    scope.launch {
                        // First let velocity push the offset slightly 
                        if (available.y != 0f) {
                           overscrollOffset.animateTo(
                               targetValue = target,
                               initialVelocity = flingVelocity,
                               animationSpec = spring(
                                   dampingRatio = Spring.DampingRatioNoBouncy,
                                   stiffness = Spring.StiffnessMedium
                               )
                           )
                        }
                        
                        // Then spring back to 0
                        overscrollOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.6f, // Bouncy
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    return Velocity(0f, available.y) // Consume Y velocity
                }
                return Velocity.Zero
            }
        }
    }

    return Modifier
        .nestedScroll(connection)
        .graphicsLayer {
            translationY = overscrollOffset.value
        }
}
