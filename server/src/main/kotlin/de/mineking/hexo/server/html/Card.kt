@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.server.html

import kotlinx.html.FlowContent
import kotlinx.html.SVG
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.svg

enum class CardColorScheme(
    val accent: String,
    val iconBoxColor: String,
    val iconColor: String,
) {
    Error(
        accent = "bg-rose-500",
        iconBoxColor = "bg-rose-50 ring-rose-200 dark:bg-rose-500/15 dark:ring-rose-400/30",
        iconColor = "text-rose-600 dark:text-rose-300",
    ) {
        override fun SVG.icon() {
            path("m18 6-12 12")
            path("m6 6 12 12")
        }
    },
    Success(
        accent = "bg-emerald-500",
        iconBoxColor = "bg-emerald-50 ring-emerald-200 dark:bg-emerald-500/15 dark:ring-emerald-400/30",
        iconColor = "text-emerald-700 dark:text-emerald-300",
    ) {
        override fun SVG.icon() {
            path("M20 6 9 17l-5-5")
        }
    },
    ;

    abstract fun SVG.icon()
}

fun FlowContent.card(color: CardColorScheme, title: String, footer: String? = null, content: FlowContent.() -> Unit) {
    section(
        classes = "w-full max-w-xl overflow-hidden rounded-lg border border-slate-200 bg-white text-slate-950 shadow-xl shadow-black/10 " +
            "dark:border-white/10 dark:bg-slate-900 dark:text-slate-50 dark:shadow-black/40",
    ) {
        div(classes = "h-1.5 ${color.accent}")
        div(classes = "p-6 sm:p-8") {
            div(classes = "mb-8 flex gap-4") {
                div(classes = "mt-0.5 inline-flex size-11 shrink-0 items-center justify-center rounded-lg ring-1 ${color.iconBoxColor}") {
                    svg(classes = "size-5 ${color.iconColor}") {
                        attributes["viewBox"] = "0 0 24 24"
                        attributes["fill"] = "none"
                        attributes["stroke"] = "currentColor"
                        attributes["stroke-width"] = "2.25"
                        attributes["stroke-linecap"] = "round"
                        attributes["stroke-linejoin"] = "round"

                        color.run {
                            icon()
                        }
                    }
                }

                div(classes = "min-w-0") {
                    p(classes = "text-[0.6rem] font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400") {
                        +"HeXO"
                    }
                    h1(classes = "mt-1 text-2xl font-semibold leading-tight") {
                        +title
                    }
                }
            }

            div(classes = "space-y-4 text-sm leading-6 text-slate-700 dark:text-slate-300") {
                content()
            }

            if (footer != null) {
                p(classes = "mt-8 border-t border-slate-200 pt-4 text-xs text-slate-500 dark:border-white/10 dark:text-slate-500") {
                    +footer
                }
            }
        }
    }
}
