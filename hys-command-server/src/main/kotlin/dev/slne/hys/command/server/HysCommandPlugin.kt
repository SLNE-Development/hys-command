package dev.slne.hys.command.server

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import dev.slne.hys.command.server.command.manager.HysCommandManager
import dev.slne.hys.command.server.command.testCommand

class HysCommandPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    override fun setup() {
        INSTANCE = this

        testCommand()

        repeat(20) {
            plugin.logger.atInfo().log("-".repeat(20))
        }
        plugin.logger.atInfo().log("Registering ${HysCommandManager.commands.size} commands...")
        HysCommandManager.registerCommandsToPlatform()
        repeat(20) {
            plugin.logger.atInfo().log("-".repeat(20))
        }
    }

    override fun start() {
        
    }

    override fun shutdown() {

    }

    companion object {
        lateinit var INSTANCE: HysCommandPlugin
            private set
    }
}

val plugin get() = HysCommandPlugin.INSTANCE