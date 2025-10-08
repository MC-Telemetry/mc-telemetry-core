package de.mctelemetry.core.neoforge

import com.mojang.brigadier.arguments.ArgumentType
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.commands.types.ArgumentTypes
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.neoforged.fml.common.Mod

@Mod(OTelCoreMod.MOD_ID)
object OTelCoreModNeoForge {

    fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>, I: ArgumentTypeInfo<A,T>>
            ArgumentTypes.PreparedArgumentTypeRegistration<A,T,I>.register() {
        ArgumentTypeInfos.registerByClass<A,T,I>(infoClass, info)
    }

    init {
        OTelCoreMod.init()
        ArgumentTypes.register {
            it.register()
        }
    }
}
