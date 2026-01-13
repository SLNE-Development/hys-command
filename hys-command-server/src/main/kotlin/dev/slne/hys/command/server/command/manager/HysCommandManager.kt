package dev.slne.hys.command.server.command.manager

import dev.slne.hys.command.server.command.HysCommand
import dev.slne.hys.command.server.plugin
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.annotations.Unmodifiable

object HysCommandManager {
    val scope =
        CoroutineScope(SupervisorJob() + CoroutineName("HysCommandManager") + CoroutineExceptionHandler { context, throwable ->

        })

    private val _commands = ObjectArraySet<HysCommand>()
    val commands: @Unmodifiable ObjectSet<HysCommand> get() = ObjectSets.unmodifiable(_commands)

    fun registerCommand(command: HysCommand) {
        _commands.add(command)
    }

    fun registerCommandsToPlatform() {
        _commands.forEach { command ->
            plugin.commandRegistry.register(command.toCommandRegistration())
        }
    }
}