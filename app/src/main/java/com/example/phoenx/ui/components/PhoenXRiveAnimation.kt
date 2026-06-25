package com.example.phoenx.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composant PhoenXRiveAnimation (ADN PHOEN-X 5.0)
 * Intégration de Rive pour le Rituel de Dépôt.
 */
@Composable
fun PhoenXRiveAnimation(
    resId: Int,
    modifier: Modifier = Modifier,
    animationName: String? = null,
    stateMachineName: String? = null,
    fit: Fit = Fit.CONTAIN,
    alignment: Alignment = Alignment.CENTER,
    autoplay: Boolean = true
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RiveAnimationView(context).apply {
                setRiveResource(
                    resId = resId,
                    animationName = animationName,
                    stateMachineName = stateMachineName,
                    fit = fit,
                    alignment = alignment,
                    autoplay = autoplay
                )
            }
        }
    )
}
