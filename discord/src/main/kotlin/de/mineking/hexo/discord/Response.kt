package de.mineking.hexo.discord

import de.mineking.discord.commands.CommandExecutor
import de.mineking.discord.commands.CommandManager
import de.mineking.discord.commands.ICommandContext
import de.mineking.discord.commands.handleException
import de.mineking.discord.commands.terminateCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.IComponent
import de.mineking.discord.ui.IdGeneratorImpl
import de.mineking.discord.ui.MenuConfig
import de.mineking.discord.ui.RenderTermination
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import java.awt.Color

private object DummyConfig : MenuConfig<Nothing, Nothing> {
    override val configState get() = error("")
    override val context get() = error("")
    override val menu get() = error("")
    override val phase get() = error("")
}

fun <C : Component> IComponent<C>.render() = render(DummyConfig, IdGeneratorImpl(""))

enum class MessageColor(val color: Color) {
    Success(Color(0x14C90E)),
    Error(Color(0xDB2B14)),
}

fun ICommandContext<*>.finalErrorResponse(content: String, component: SectionAccessoryComponent? = null): Nothing = finalResponse(MessageColor.Error, content, component)
fun ICommandContext<*>.finalSuccessResponse(content: String, component: SectionAccessoryComponent? = null): Nothing = finalResponse(MessageColor.Success, content, component)
fun ICommandContext<*>.finalResponse(color: MessageColor, content: String, component: SectionAccessoryComponent? = null): Nothing {
    respond(color, content, component)
    terminateCommand()
}

fun IReplyCallback.respond(color: MessageColor, content: String, component: SectionAccessoryComponent? = null) {
    val rendered = render(color, content, component)

    if (isAcknowledged) hook.editOriginalComponents(rendered).setReplace(true).queue()
    else {
        if (this is IMessageEditCallback) editComponents(rendered).setReplace(true).queue()
        else replyComponents(rendered).setEphemeral(true).queue()
    }
}

fun render(color: MessageColor, content: String, component: SectionAccessoryComponent? = null): Container {
    val text = TextDisplay.of(content)
    val content = if (component != null) Section.of(component, text) else text

    return Container.of(content).withAccentColor(color.color)
}

interface ErrorHandlingLocalization : LocalizationFile {
    @Localize
    fun responseCommandErrorUnknown(@Locale locale: DiscordLocale): String
}

fun CommandManager.installErrorHandling(localization: ErrorHandlingLocalization) {
    execute(CommandExecutor.DEFAULT.handleException<Exception> { _, e ->
        logger.error(e) { "Unexpected error during command execution" }
        finalErrorResponse(localization.responseCommandErrorUnknown(userLocale))
    })
}
