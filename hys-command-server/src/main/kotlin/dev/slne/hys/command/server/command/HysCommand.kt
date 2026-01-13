package dev.slne.hys.command.server.command

import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandRegistration
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.console.ConsoleSender
import com.hypixel.hytale.server.core.entity.entities.Player
import dev.slne.hys.command.server.command.manager.HysCommandManager
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

fun hysCommand(
    name: String,
    description: String,
    requiresConfirmation: Boolean = false,
    init: HysCommand.() -> Unit
): HysCommand {
    val command = object : HysCommand(name, description, requiresConfirmation) {}
    command.init()
    return command.register()
}

fun hysSubCommand(
    name: String,
    description: String,
    requiresConfirmation: Boolean = false,
    init: HysCommand.() -> Unit
): HysCommand {
    val command = object : HysCommand(name, description, requiresConfirmation) {}
    command.init()
    return command
}

abstract class HysCommand(
    val name: String,
    val description: String,
    val requiresConfirmation: Boolean = false
) {
    private val allowsExtraArguments = false
    private val aliases = ObjectArraySet<String>()
    private val subCommands = ObjectArraySet<HysCommand>()

    private var anyExecutor: (suspend (sender: CommandSender, context: CommandContext) -> Unit)? =
        null
    private var consoleExecutor: (suspend (sender: ConsoleSender, context: CommandContext) -> Unit)? =
        null
    private var playerExecutor: (suspend (sender: Player, context: CommandContext) -> Unit)? = null

    var unregisterHook: (() -> Unit)? = null
        private set

    var enabled: Boolean = true
        private set

    fun register(): HysCommand {
        HysCommandManager.registerCommand(this)

        return this
    }

    private fun requireNoOtherExecutorsSet() {
        if (anyExecutor != null || consoleExecutor != null || playerExecutor != null) {
            throw IllegalStateException("An executor is already defined for command '$name'")
        }
    }

    fun isEnabled(enabled: Boolean): HysCommand {
        this.enabled = enabled

        return this
    }

    fun setEnabled(enabled: Boolean): HysCommand {
        this.enabled = enabled

        return this
    }

    fun setDisabled(disabled: Boolean): HysCommand {
        this.enabled = !disabled

        return this
    }

    fun withUnregisterHook(hook: () -> Unit): HysCommand {
        this.unregisterHook = hook

        return this
    }

    fun anyExecutor(
        function: suspend (sender: CommandSender, context: CommandContext) -> Unit
    ): HysCommand {
        requireNoOtherExecutorsSet()

        this.anyExecutor = function

        return this
    }

    fun consoleExecutor(
        function: suspend (sender: ConsoleSender, context: CommandContext) -> Unit
    ): HysCommand {
        requireNoOtherExecutorsSet()

        this.consoleExecutor = function

        return this
    }

    fun playerExecutor(
        function: suspend (sender: Player, context: CommandContext) -> Unit
    ): HysCommand {
        requireNoOtherExecutorsSet()

        this.playerExecutor = function

        return this
    }

    fun withSubCommand(subCommand: HysCommand): HysCommand {
        this.subCommands.add(subCommand)

        return this
    }

    fun toCommandRegistration(): CommandRegistration {
        return CommandRegistration(toPlatformCommand(), { enabled }, { unregisterHook?.invoke() })
    }

    private fun toPlatformCommand(): AbstractCommand {
        return object : AbstractCommand(name, description, requiresConfirmation) {
            init {
                addAliases(*this@HysCommand.aliases.toTypedArray())
                setAllowsExtraArguments(this@HysCommand.allowsExtraArguments)

                this@HysCommand.subCommands.forEach { subCommand ->
                    addSubCommand(subCommand.toPlatformCommand())
                }
            }

            override fun execute(context: CommandContext): CompletableFuture<Void> {
                val future = CompletableFuture<Void>()

                HysCommandManager.scope.launch {
                    val commandExecutor = context.sender()

                    val anyExecutor = anyExecutor
                    val consoleExecutor = consoleExecutor
                    val playerExecutor = playerExecutor

                    if (anyExecutor != null) {
                        invokeExecutor(context, anyExecutor)
                    } else if (commandExecutor is ConsoleSender && consoleExecutor != null) {
                        invokeExecutor(context) { sender, ctx ->
                            consoleExecutor.invoke(sender as ConsoleSender, ctx)
                        }
                    } else if (commandExecutor is Player && playerExecutor != null) {
                        invokeExecutor(context) { sender, ctx ->
                            playerExecutor.invoke(sender as Player, ctx)
                        }
                    } else {
                        throw IllegalStateException("No suitable executor found for command '$name'")
                    }

                    future.complete(null)
                }

                return future
            }

            private suspend fun invokeExecutor(
                context: CommandContext,
                function: suspend (sender: CommandSender, context: CommandContext) -> Unit
            ) {
                try {
                    function(context.sender(), context)
                } catch (exception: Exception) {
                    throw exception
                }
            }
        }
    }
}