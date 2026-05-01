@file:Suppress("MatchingDeclarationName", "Filename")

package de.mineking.hexo.api.game

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class LobbyId(val value: String)
