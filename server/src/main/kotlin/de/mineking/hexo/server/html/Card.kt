@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.server.html

import kotlinx.html.FlowContent
import kotlinx.html.SVG
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.svg

enum class CardColorScheme(val gradient: String, val iconBoxColor: String, val iconColor: String) {
    Error(gradient = "from-rose-400 to-red-500", iconBoxColor = "bg-rose-500/15 ring-rose-400/30", iconColor = "text-rose-400") {
        override fun SVG.icon() {
            path("m18 6-12 12")
            path("m6 6 12 12")
        }
    },
    Success(gradient = "from-emerald-400 to-green-500", iconBoxColor = "bg-emerald-500/15 ring-emerald-400/30", iconColor = "text-emerald-400") {
        override fun SVG.icon() {
            path("M20 6 9 17l-5-5")
        }
    },
    ;

    abstract fun SVG.icon()
}

fun FlowContent.card(color: CardColorScheme, title: String, footer: String? = null, content: FlowContent.() -> Unit) {
    section(classes = "w-full max-w-lg overflow-hidden rounded-2xl border border-white/10 bg-slate-900/80 shadow-2xl shadow-black/50") {
        div(classes = "h-1.5 bg-linear-to-r ${color.gradient}")
        div(classes = "p-8") {
            div(classes = "flex grow gap-4 mb-8 items-center") {
                div(classes = "ml-px inline-flex size-12 items-center justify-center rounded-xl ring-1 ${color.iconBoxColor}") {
                    svg(classes = "size-6 ${color.iconColor}") {
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
                h1(classes = "text-2xl font-semibold tracking-tight") {
                    +title
                }
            }
            p(classes = "mt-3 text-sm leading-6 text-slate-300") {
                content()
            }
            if (footer != null) {
                p(classes = "mt-6 text-xs text-slate-500") {
                    +footer
                }
            }
        }
    }
}
