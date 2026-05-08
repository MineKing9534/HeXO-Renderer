@file:Suppress("MatchingDeclarationName", "Filename")

package de.mineking.hexo.api

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ProfileId(val value: String)
