package de.mineking.hexo.web.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun LoadingIndicator(attrs: AttrBuilderContext<HTMLDivElement>? = null) {
    Div({
        classes(
            "animate-spin",
            "rounded-full",
            "border-4",
            "border-emerald-600",
            "border-t-slate-600",
        )
        attrs?.invoke(this)
    })
}
