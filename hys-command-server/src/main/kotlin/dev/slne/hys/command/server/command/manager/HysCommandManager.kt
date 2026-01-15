package dev.slne.hys.command.server.command.manager

import com.hypixel.hytale.server.core.command.system.CommandManager
import dev.slne.hys.command.server.command.HysCommand
import dev.slne.hys.command.server.plugin
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.annotations.Unmodifiable

object HysCommandManager {
    private val log = logger()

    val scope =
        CoroutineScope(SupervisorJob() + CoroutineName("HysCommandManager") + CoroutineExceptionHandler { context, throwable ->
            log.atSevere().withCause(throwable).log(
                "Unhandled exception in HysCommandManager coroutine context: $context"
            )
        })

    private val _commands = ObjectArraySet<HysCommand>()
    val commands: @Unmodifiable ObjectSet<HysCommand> get() = ObjectSets.unmodifiable(_commands)

    fun registerCommand(command: HysCommand) {
        _commands.add(command)
    }

    fun registerCommandsToPlatform() {
        _commands.forEach { command ->
            CommandManager.get().register(command.toCommand())
            plugin.logger.atInfo().log("Registered command: /${command.name}")
        }
    }
}