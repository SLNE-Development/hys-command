package dev.slne.hys.command.server.command

import com.hypixel.hytale.server.core.Message

fun testCommand() = hysCommand("test", "A test command") {
    withSubCommand(testSubCommand())

    anyExecutor { sender, context ->
        sender.sendMessage(Message.raw("You executed the test command!"))
    }
}

fun HysCommand.testSubCommand() = hysSubCommand("sub", "A test subcommand") {
    anyExecutor { sender, context ->
        sender.sendMessage(Message.raw("You executed the test subcommand!"))
    }
}