package dev.slne.shy.command.server.command

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandRegistration
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.console.ConsoleSender
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.PluginClassLoader
import dev.slne.shy.command.server.command.manager.ShyCommandManager
import dev.slne.shy.command.server.command.reflection.Reflection
import dev.slne.surf.surfapi.core.api.util.getCallerClass
import dev.slne.surf.surfapi.hytale.api.coroutines.scope
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

fun shyCommand(
    name: String,
    description: String,
    requiresConfirmation: Boolean = false,
    owner: JavaPlugin? = null,
    init: ShyCommand.() -> Unit
): ShyCommand {
    val ownerPlugin = determinePlugin(owner)
    val command = object : ShyCommand(ownerPlugin, name, description, requiresConfirmation) {}

    command.init()

    return command.register()
}

fun shySubCommand(
    name: String,
    description: String,
    requiresConfirmation: Boolean = false,
    owner: JavaPlugin? = null,
    init: ShyCommand.() -> Unit
): ShyCommand {
    val ownerPlugin = determinePlugin(owner)
    val command = object : ShyCommand(ownerPlugin, name, description, requiresConfirmation) {}

    command.init()

    return command
}

private fun determinePlugin(plugin: JavaPlugin?): JavaPlugin {
    return if (plugin != null) {
        plugin
    } else {
        val classLoader = getCallerClass(1)?.classLoader as? PluginClassLoader
            ?: error("Could not determine calling plugin classloader")

        Reflection.PLUGIN_CLASSLOADER_PROXY.getPlugin(classLoader)
            ?: error("$classLoader plugin not found")
    }
}

abstract class ShyCommand(
    val owner: JavaPlugin,
    val name: String,
    val description: String,
    val requiresConfirmation: Boolean = false
) {
    private val allowsExtraArguments = false
    private val aliases = ObjectArraySet<String>()
    private val subCommands = ObjectArraySet<ShyCommand>()

    private var executor: Pair<ExecutorType, (suspend CoroutineScope.(sender: CommandSender, context: CommandContext) -> Unit)>? =
        null

    var unregisterHook: (() -> Unit)? = null
        private set

    var enabled: Boolean = true
        private set

    fun register(): ShyCommand {
        ShyCommandManager.registerCommand(this)

        return this
    }

    fun isEnabled(enabled: Boolean): ShyCommand {
        this.enabled = enabled

        return this
    }

    fun setEnabled(enabled: Boolean): ShyCommand {
        this.enabled = enabled

        return this
    }

    fun setDisabled(disabled: Boolean): ShyCommand {
        this.enabled = !disabled

        return this
    }

    fun withUnregisterHook(hook: () -> Unit): ShyCommand {
        this.unregisterHook = hook

        return this
    }

    private fun requireNoOtherExecutorsSet() {
        if (executor != null) {
            throw IllegalStateException("Executor already set for command '$name'")
        }
    }

    fun anyExecutor(
        function: suspend CoroutineScope.(sender: CommandSender, context: CommandContext) -> Unit
    ): ShyCommand {
        requireNoOtherExecutorsSet()

        this.executor = Pair(ExecutorType.ANY, function)

        return this
    }

    fun consoleExecutor(
        function: suspend CoroutineScope.(sender: ConsoleSender, context: CommandContext) -> Unit
    ): ShyCommand {
        requireNoOtherExecutorsSet()

        this.executor = Pair(ExecutorType.CONSOLE) { sender, context ->
            function(sender as ConsoleSender, context)
        }

        return this
    }

    fun playerExecutor(
        function: suspend CoroutineScope.(sender: Player, context: CommandContext) -> Unit
    ): ShyCommand {
        requireNoOtherExecutorsSet()

        this.executor = Pair(ExecutorType.PLAYER) { sender, context ->
            function(sender as Player, context)
        }

        return this
    }

    fun withSubCommand(subCommand: ShyCommand): ShyCommand {
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
                addAliases(*this@ShyCommand.aliases.toTypedArray())
                setAllowsExtraArguments(this@ShyCommand.allowsExtraArguments)
                setOwner(this@ShyCommand.owner)

                this@ShyCommand.subCommands.forEach { subCommand ->
                    addSubCommand(subCommand.toCommand())
                }
            }
        }
    }

    private fun toAbstractCommand(): AbstractAsyncCommand {
        return object : AbstractAsyncCommand(name, description, requiresConfirmation) {
            init {
                addAliases(*this@ShyCommand.aliases.toTypedArray())
                setAllowsExtraArguments(this@ShyCommand.allowsExtraArguments)
                setOwner(this@ShyCommand.owner)
            }

            override fun executeAsync(context: CommandContext): CompletableFuture<Void?> {
                return this@ShyCommand.owner.scope.future {
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
}