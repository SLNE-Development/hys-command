package dev.slne.shy.command.server.command.manager

import com.hypixel.hytale.server.core.command.system.CommandManager
import dev.slne.shy.command.server.command.ShyCommand
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import org.jetbrains.annotations.Unmodifiable

object ShyCommandManager {
    private val _commands = ObjectArraySet<ShyCommand>()
    val commands: @Unmodifiable ObjectSet<ShyCommand> get() = ObjectSets.unmodifiable(_commands)

    fun registerCommand(command: ShyCommand) {
        _commands.add(command)
    }

    fun registerCommandsToPlatform() {
        _commands.forEach { command ->
            CommandManager.get().register(command.toCommand())
        }
    }
}