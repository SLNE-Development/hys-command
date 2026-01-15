package dev.slne.shy.command.server.command.reflection.proxy

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.PluginClassLoader
import dev.slne.surf.surfapi.core.api.reflection.Field
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy

@SurfProxy(PluginClassLoader::class)
interface PluginClassloaderProxy {

    @Field(name = "plugin", type = Field.Type.GETTER)
    fun getPlugin(instance: PluginClassLoader): JavaPlugin?
}