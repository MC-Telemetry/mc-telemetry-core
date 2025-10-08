package de.mctelemetry.core.fabric

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.exporters.metrics.MetricAccessor
import de.mctelemetry.core.exporters.metrics.MetricsGameReader
import net.fabricmc.api.ModInitializer
import java.lang.reflect.AccessFlag
import kotlin.reflect.jvm.javaMethod

object OTelCoreModFabric : ModInitializer {

    init {
        val className = "de.mctelemetry.core.exporters.metrics.MetricAccessor"//MetricAccessor::class.java.canonicalName
        println("MetricAccessor-Class: $className")
        println("Mod-SystemClassLoader: ${ClassLoader.getSystemClassLoader()}")
        val getInstanceResult = ClassLoader.getSystemClassLoader().loadClass(className)!!.getDeclaredMethod("getInstance")
            .also {
                require(AccessFlag.STATIC in it.accessFlags()) {"getInstance Method is not static: ${it.accessFlags()}"}
            }.invoke(null)
        println("MetricAccessor.getInstance: ${getInstanceResult}")
    }

    override fun onInitialize() {
        OTelCoreMod.init()
        println("MetricAccessor: ${MetricAccessor.INSTANCE}")

    }
}
