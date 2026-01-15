package dev.slne.hys.command.server.command

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandRegistration
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.console.ConsoleSender
import com.hypixel.hytale.server.core.entity.entities.Player
import dev.slne.hys.command.server.command.manager.HysCommandManager
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import kotlinx.coroutines.future.future
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

    private var executor: Pair<ExecutorType, (suspend (sender: CommandSender, context: CommandContext) -> Unit)>? =
        null

    var unregisterHook: (() -> Unit)? = null
        private set

    var enabled: Boolean = true
        private set

    fun register(): HysCommand {
        HysCommandManager.registerCommand(this)

        return this
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

    private fun requireNoOtherExecutorsSet() {
        if (executor != null) {
            throw IllegalStateException("Executor already set for command '$name'")
        }
    }

    fun anyExecutor(
        function: suspend (sender: CommandSender, context: CommandContext) -> Unit
    ): HysCommand {
        requireNoOtherExecutorsSet()

        this.executor = Pair(ExecutorType.ANY, function)

        return this
    }

    fun consoleExecutor(
        function: suspend (sender: ConsoleSender, context: CommandContext) -> Unit
    ): HysCommand {
        requireNoOtherExecutorsSet()

        this.executor = Pair(ExecutorType.CONSOLE) { sender, context ->
            function(sender as ConsoleSender, context)
        }

        return this
    }

    fun playerExecutor(
        function: suspend (sender: Player, context: CommandContext) -> Unit
    ): HysCommand {
        requireNoOtherExecutorsSet()

        this.executor = Pair(ExecutorType.PLAYER) { sender, context ->
            function(sender as Player, context)
        }

        return this
    }

    fun withSubCommand(subCommand: HysCommand): HysCommand {
        this.subCommands.add(subCommand)

        return this
    }

    fun toCommandRegistration(): CommandRegistration {
        return CommandRegistration(toAbstractCommand(), { enabled }, { unregisterHook?.invoke() })
    }

    fun toCommand(): AbstractAsyncCommand {
        return if (subCommands.isEmpty()) {
            toAbstractCommand()
        } else {
            toAbstractCommandCollection()
        }
    }

    private fun toAbstractCommandCollection(): AbstractCommandCollection {
        return object : AbstractCommandCollection(name, description) {
            init {
                addAliases(*this@HysCommand.aliases.toTypedArray())
                setAllowsExtraArguments(this@HysCommand.allowsExtraArguments)

                this@HysCommand.subCommands.forEach { subCommand ->
                    addSubCommand(subCommand.toCommand())
                }
            }
        }
    }

    private fun toAbstractCommand(): AbstractAsyncCommand {
        return object : AbstractAsyncCommand(name, description, requiresConfirmation) {
            init {
                addAliases(*this@HysCommand.aliases.toTypedArray())
                setAllowsExtraArguments(this@HysCommand.allowsExtraArguments)
            }

            override fun executeAsync(context: CommandContext): CompletableFuture<Void?> =
                HysCommandManager.scope.future {
                    val executor = executor
                        ?: throw IllegalStateException("No executor defined for command '${name}'")

                    val (executorType, executorFunction) = executor
                    val sender = context.senderAs(executorType.clazz)

                    executorFunction(sender, context)

                    null
                }
        }
    }
}