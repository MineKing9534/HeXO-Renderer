package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.mineking.hexo.api.asBoard
import de.mineking.hexo.api.formation.FormationId
import de.mineking.hexo.api.formation.FormationRepository
import de.mineking.hexo.api.game.FinishedGameRepository
import de.mineking.hexo.api.game.GameId
import de.mineking.hexo.board.Board
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.LoadingIndicator
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun ImportDialog(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    onClose: () -> Unit,
    onConfirm: (Board) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val valid = remember(url, error) { url.matches(FORMATION_URL) || url.matches(GAME_URL) || error }

    Dialog(
        title = "Import Position",
        onClose = onClose,
        actionRow = {
            ConfirmButton(
                formationRepository = formationRepository,
                finishedGameRepository = finishedGameRepository,
                url = url,
                valid = valid,
                onErrorUpdate = { error = it },
                onConfirm = onConfirm,
            )
        },
    ) {
        Div({ classes("text-sm", "font-semibold", "uppercase", "text-slate-300") }) {
            Text("URL")
        }
        Div({ classes("text-xs", "text-slate-500") }) {
            Text("Game or Sandbox Position Link")
        }

        UrlInput(
            url = url,
            onUrlUpdate = {
                url = it
                error = false
            },
            valid = valid,
        )

        if (error) {
            Div({ classes("min-h-5", "text-sm", "leading-relaxed", "text-rose-400") }) {
                Text("Position or game not found")
            }
        }
    }
}

@Composable
private fun UrlInput(url: String, onUrlUpdate: (String) -> Unit, valid: Boolean) {
    Input(InputType.Url) {
        value(url)
        placeholder("https://hexo.did.science/sandbox/2mdyn02")
        onInput {
            onUrlUpdate(it.value)
        }
        classes(
            "w-full", "rounded-lg", "border-3", "border-slate-700", "bg-slate-950", "p-3",
            "text-sm", "text-slate-100", "outline-none", "transition", "focus:bg-slate-800", "text-ellipsis",
        )
        if (!valid) {
            classes("focus:border-rose-400")
        } else {
            classes("focus:border-emerald-400")
        }
    }
    Div({ classes("text-xs", "text-slate-500") }) {
        Span({ classes("font-bold", "uppercase") }) {
            Text("Hint: ")
        }
        Text("You can add ")
        Span({
            classes(
                "mx-1", "inline-flex", "items-center", "rounded-md", "border", "px-1.5", "py-0.5", "font-mono", "text-xs", "font-semibold",
                "border-indigo-300/30", "bg-slate-950/60", "text-indigo-300",
            )
        }) {
            Text("?move=...")
        }
        Text(" to a game url to import the position at a specific move!")
    }
}

@Composable
private fun ConfirmButton(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
    url: String,
    valid: Boolean,
    onErrorUpdate: (Boolean) -> Unit,
    onConfirm: (Board) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    Button({
        if (!valid) disabled()

        classes(
            "rounded-lg", "border", "px-4", "py-2", "transition", "text-sm", "font-medium", "h-10", "w-20",
            "flex", "items-center", "justify-center",
        )

        if (loading || !valid) {
            classes("border-slate-500/40", "bg-slate-500/15", "text-slate-300")
        } else {
            classes("border-emerald-500/40", "bg-emerald-500/15", "text-emerald-300", "hover:bg-emerald-500/25", "hover:text-emerald-100")
        }

        onClick {
            loading = true
            onErrorUpdate(false)
            coroutineScope.launch {
                try {
                    val board = url.urlToBoard(formationRepository, finishedGameRepository)
                    if (board == null) {
                        onErrorUpdate(true)
                        return@launch
                    }

                    onConfirm(board)
                } finally {
                    loading = false
                }
            }
        }
    }) {
        if (loading) {
            LoadingIndicator { classes("size-6") }
        } else {
            Text("Import")
        }
    }
}

private val FORMATION_URL = """^$URL/sandbox/(.*)$""".toRegex()
private val GAME_URL = """^$URL/games/(.*?)(?:\?move=(\d+))?$""".toRegex()
private suspend fun String.urlToBoard(
    formationRepository: FormationRepository,
    finishedGameRepository: FinishedGameRepository,
): Board? {
    val formationMatch = FORMATION_URL.matchEntire(this)
    val gameMatch = GAME_URL.matchEntire(this)

    return when {
        formationMatch != null -> formationRepository.getFormation(FormationId(formationMatch.groupValues[1]))?.asBoard()
        gameMatch != null -> {
            val move = gameMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toInt()
            finishedGameRepository.getGame(GameId(gameMatch.groupValues[1]))?.asBoard(move ?: Int.MAX_VALUE)
        }
        else -> null
    }
}
