package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import de.mctelemetry.core.network.observations.container.observationrequest.ObservationRequestManagerClient
import de.mctelemetry.core.network.observations.container.observationrequest.ObservationSourceObservationMap
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.ui.datacomponents.ObservationValuePreviewDataComponent
import de.mctelemetry.core.ui.datacomponents.ObservationValueStateDataComponent
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.closeConsumeAllRethrow
import de.mctelemetry.core.utils.coroutineDispatcher
import de.mctelemetry.core.utils.globalPosOrThrow
import de.mctelemetry.core.utils.runWithExceptionCleanup
import de.mctelemetry.core.utils.toShortString
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.FlowLayout
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.core.GlobalPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.atomic.AtomicReference

@Environment(EnvType.CLIENT)
class ScraperBlockScreen(
    private val globalPos: GlobalPos,
    private val observationSourceContainer: ObservationSourceContainer<*>,
    private val titleComponent: Component
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing"
        )
    )
), AutoCloseable {

    constructor(entity: ObservationSourceContainerBlockEntity) : this(
        entity.globalPosOrThrow, entity.container,
        Component.translatable(entity.blockState.blockHolder.unwrapKey().get().location().toLanguageKey("block"))
    )

    private val scope: CoroutineScope = CoroutineScope(Minecraft.getInstance().coroutineDispatcher)
    private val observationFlowRef: AtomicReference<Deferred<StateFlow<ObservationSourceObservationMap>>?> =
        AtomicReference(null)

    init {
        OTelCoreMod.logger.trace("Opened new coroutine scope for {}", this)
    }

    private val observationValuePreviews: Byte2ObjectMap<ObservationValuePreviewDataComponent> =
        Byte2ObjectOpenHashMap()

    private val closable = mutableListOf<AutoCloseable>()

    override fun isPauseScreen(): Boolean {
        return false
    }

    private fun acceptObservations(observationBundle: ObservationSourceObservationMap) {
        for ((source, observations) in observationBundle) {
            val preview = observationValuePreviews.getOrElse(source.second.toByte()) {
                if (observationValuePreviews.isEmpty()) {
                    OTelCoreMod.logger.trace(
                        "No value previews initialized while applying data for observation source {}/{}, skipping further data",
                        source.first,
                        source.second,
                    )
                    return
                } else {
                    OTelCoreMod.logger.warn(
                        "Could not find matching value preview for received observation source {}/{}",
                        source.first,
                        source.second,
                    )
                    continue
                }
            }

            preview.value = observations
        }
    }

    private fun configureFlow(): Deferred<StateFlow<ObservationSourceObservationMap>> {
        observationFlowRef.get()?.let { return it }
        val observationManager = ObservationRequestManagerClient.getActiveManager()
        val deferred = scope.async(start = CoroutineStart.LAZY) {
            val observationFlow = observationManager.requestObservations(globalPos, 20u)
            scope.launch(CoroutineName("${ScraperBlockScreen::class.java.simpleName}(${globalPos.toShortString()}).uiUpdater")) {
                observationFlow.collect(::acceptObservations)
            }
            observationFlow
        }
        runWithExceptionCleanup(deferred::cancel) {
            val existingValue = observationFlowRef.compareAndExchange(null, deferred)
            if (existingValue != null) {
                deferred.cancel()
                return existingValue
            } else {
                deferred.start()
                return deferred
            }
        }
    }

    override fun added() {
        @Suppress("DeferredResultUnused")
        configureFlow()
    }

    override fun close() {
        try {
            scope.cancel()
        } finally {
            closable.closeConsumeAllRethrow()
        }
        OTelCoreMod.logger.trace("Cancelled coroutine scope for {}", this)
    }

    override fun onClose() {
        @Suppress("ConvertTryFinallyToUseCall")
        try {
            super.onClose()
        } finally {
            this.close()
        }
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent.childByIdOrThrow<LabelComponent>("title").text(titleComponent)

        val instrumentManagerButton: ButtonComponent = rootComponent.childWidgetByIdOrThrow("instrument-manager")
        instrumentManagerButton.onPress {
            val instrumentManager = ClientInstrumentMetaManager.activeWorldManager ?: return@onPress
            Minecraft.getInstance().setScreen(InstrumentManagerScreen(instrumentManager))
        }

        val list: FlowLayout = rootComponent.childByIdOrThrow("list")

        for (entry in observationSourceContainer.observationStates.byte2ObjectEntrySet()) {
            val state = entry.value
            val instanceID = entry.byteKey.toUByte()
            val template = model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:source-listing",
                mapOf()
            )
            template.childByIdOrThrow<LabelComponent>("observation-source-name").text(
                TranslationKeys.ObservationSources[state.source]
            )
            list.child(template)

            closable.add(
                ObservationValueStateDataComponent(
                    template.childByIdOrThrow<LabelComponent>("observation-source-state"),
                    state
                )
            )

            observationValuePreviews[instanceID.toByte()] = ObservationValuePreviewDataComponent(
                template.childByIdOrThrow<LabelComponent>("observation-source-value")
            )

            val editButton: ButtonComponent = template.childWidgetByIdOrThrow("observation-source-edit")
            editButton.onPress {
                Minecraft.getInstance().setScreen(ScraperBlockScreenDetails(this, globalPos, state))
            }
        }

        observationFlowRef.get()?.let { observationFlow ->
            if (!observationFlow.isCompleted) return@let
            scope.launch {
                acceptObservations(observationFlow.await().value)
            }
        }
    }
}
