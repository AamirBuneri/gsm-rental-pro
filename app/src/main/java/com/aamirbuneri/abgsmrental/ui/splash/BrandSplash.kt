package com.aamirbuneri.abgsmrental.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aamirbuneri.abgsmrental.R
import com.aamirbuneri.abgsmrental.ui.theme.AB
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Full-screen brand art (dark art in dark mode, white art in light mode) shown once per launch. */
@Composable
fun BrandSplash(dark: Boolean, onDone: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1.06f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            listOf(
                async { alpha.animateTo(1f, tween(450)) },
                async { scale.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) },
                async { progress.animateTo(1f, tween(1500, easing = LinearEasing)) },
            ).awaitAll()
        }
        onDone()
    }
    Box(Modifier.fillMaxSize().background(if (dark) Color(0xFF05080E) else Color.White)) {
        Image(
            painter = painterResource(if (dark) R.drawable.splash_art_dark else R.drawable.splash_art_light),
            contentDescription = "AB Gsm Rental",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        // thin loading line under the art
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .width(140.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.value)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(AB.brand.gradient),
            )
        }
    }
}
