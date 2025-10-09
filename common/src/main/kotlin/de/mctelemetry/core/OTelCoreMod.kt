package de.mctelemetry.core

import de.mctelemetry.core.commands.scrape.CommandScrape
import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import dev.architectury.event.events.common.CommandRegistrationEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.message.SimpleMessageFactory
import kotlin.collections.component1
import kotlin.collections.component2

object OTelCoreMod {

    const val MOD_ID = "mcotelcore"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    fun init() {
        debugMetrics()
        CommandRegistrationEvent.EVENT.register { evt,a,b ->
            evt.root.addChild(buildCommand("mcotel") {
                then(CommandScrape().command)
            })
        }
    }

    init {
        logger.debug {
            SimpleMessageFactory.INSTANCE.newMessage(
                "ClassLoader during mod class loading: {}",
                OTelCoreMod::class.java.classLoader
            )
        }
        val metricsAccessor = MetricsAccessor.INSTANCE
        logger.debug("MetricsAccessor during mod class loading: {}", metricsAccessor)
        if (metricsAccessor != null) {
            logger.info("Performing initial metrics collection, may throw errors which can be safely ignored (probably?)")
            metricsAccessor.collect()
            logger.info("Initial metric collection done, any errors following this should be treated as errors again")
        } else {
            logger.debug("Could not find MetricsAccessor during Mod-Init.")
        }
    }

    private fun debugMetrics() {
        val accessor = MetricsAccessor.INSTANCE
        if (accessor != null) {
            logger.info("Metrics: ")
            accessor.collect().forEach { (k, v) ->
                logger.info(
                    """
                    |$k:
                    |   TYPE: ${v.type}
                    |   UNIT: ${v.unit}
                    |   DESCRIPTION: ${v.description}
                    |   DATA: ${v.data}
                    """.trimIndent().replaceIndentByMargin("    ")
                )
            }
        } else {
            logger.info("Metrics not configured")
        }
    }
}
