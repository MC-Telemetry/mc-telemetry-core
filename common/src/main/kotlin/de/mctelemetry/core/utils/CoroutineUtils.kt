package de.mctelemetry.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import java.util.concurrent.Executor

@get:Environment(EnvType.CLIENT)
val Minecraft.coroutineDispatcher: CoroutineDispatcher by ArgLazy(Executor::asCoroutineDispatcher)

val MinecraftServer.coroutineDispatcher: CoroutineDispatcher by ArgLazy(Executor::asCoroutineDispatcher)
