package dev.slne.shy.command.server

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import dev.slne.shy.command.server.command.manager.ShyCommandManager

class ShyCommandPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    override fun setup() {
        INSTANCE = this
    }

    override fun start() {
        ShyCommandManager.registerCommandsToPlatform()
    }

    override fun shutdown() {

    }

    companion object {
        lateinit var INSTANCE: ShyCommandPlugin
            private set
    }
}

val plugin get() = ShyCommandPlugin.INSTANCE