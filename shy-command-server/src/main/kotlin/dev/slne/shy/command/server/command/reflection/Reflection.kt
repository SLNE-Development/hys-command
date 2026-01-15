package dev.slne.shy.command.server.command.reflection

import dev.slne.shy.command.server.command.reflection.proxy.PluginClassloaderProxy
import dev.slne.surf.surfapi.core.api.reflection.createProxy
import dev.slne.surf.surfapi.core.api.reflection.surfReflection

object Reflection {
    val PLUGIN_CLASSLOADER_PROXY = surfReflection.createProxy<PluginClassloaderProxy>()
}