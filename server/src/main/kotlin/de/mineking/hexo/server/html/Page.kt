package de.mineking.hexo.server.html

import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.MAIN
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.title

fun HTML.page(title: String, content: MAIN.() -> Unit) {
    head {
        title(title)
        link(rel = "stylesheet", href = "/static/styles.css")
        link(rel = "icon", type = "image/x-icon", href = "/static/favicon.png")
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        meta(name = "color-scheme", content = "light dark")
    }

    body(classes = "min-h-screen bg-slate-950 text-slate-100 antialiased") {
        main(classes = "min-h-screen px-5 py-10 grid place-items-center sm:px-6") {
            content()
        }
    }
}

fun HTML.statusPage(color: CardColorScheme, title: String, footer: String? = null, content: FlowContent.() -> Unit) {
    page(title) {
        card(color, title, footer, content)
    }
}
