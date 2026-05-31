package de.mineking.hexo.server.html

import kotlinx.html.HTMLTag
import kotlinx.html.SVG
import kotlinx.html.TagConsumer
import kotlinx.html.visit

private const val SVG_NS = "http://www.w3.org/2000/svg"

fun SVG.path(d: String) {
    PATH(mapOf("d" to d), consumer).visit {}
}

@Suppress("unused")
open class PATH(
    initialAttributes: Map<String, String>,
    override val consumer: TagConsumer<*>,
) : HTMLTag(
    tagName = "path",
    consumer = consumer,
    initialAttributes = initialAttributes,
    namespace = SVG_NS,
    inlineTag = false,
    emptyTag = false,
)
