package com.mctelemetry.core

import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.runForDist

@Mod("mctelemetrycore")
object MCTelemetryCoreMod {

    val LOGGER: Logger = LogManager.getLogger("mctelemetrycore")

    init {
        LOGGER.log(Level.INFO, "Hello world!")
        val bus = FMLJavaModLoadingContext.get().modEventBus
        FORGE_BUS.addListener(::onRegisterCommands)

        runForDist(
            clientTarget = {
                bus.addListener(::onClientSetup)
            },
            serverTarget = {
                bus.addListener(::onServerSetup)
            })

    }

    private fun onRegisterCommands(event: RegisterCommandsEvent){
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        //LogUtils.configureRootLoggingLevel(org.slf4j.event.Level.DEBUG)
        LOGGER.log(Level.INFO, "Initializing client...")
    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }
}
