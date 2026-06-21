package com.example.webviewbrowser.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Chrome系UIに合わせた、不透明でコントラストが安定するカードサーフェス。 */
@Composable
fun Modifier.appSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    selected: Boolean = false,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val color = if (selected) scheme.primaryContainer else scheme.surfaceContainer
    return clip(shape)
        .background(color)
        .border(1.dp, scheme.outlineVariant, shape)
}

/** 主要画面で共通利用する単色背景。 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        content = content,
    )
}
