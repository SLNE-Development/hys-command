package dev.slne.shy.command.server.command

import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.console.ConsoleSender
import com.hypixel.hytale.server.core.entity.entities.Player

enum class ExecutorType(
    val clazz: Class<out CommandSender>,
) {
    ANY(CommandSender::class.java),
    PLAYER(Player::class.java),
    CONSOLE(ConsoleSender::class.java);
}