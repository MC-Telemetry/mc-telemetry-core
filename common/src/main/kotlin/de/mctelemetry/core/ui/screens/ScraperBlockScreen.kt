package de.mctelemetry.core.ui.screens

import com.mojang.brigadier.StringReader
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.IParameterizedObservationSource
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import de.mctelemetry.core.network.observations.container.observationrequest.ObservationRequestManagerClient
import de.mctelemetry.core.network.observations.container.observationrequest.ObservationSourceObservationMap
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservations
import de.mctelemetry.core.network.observations.container.sync.C2SObservationSourceStateAddPayload
import de.mctelemetry.core.network.observations.container.sync.C2SObservationSourceStateRemovePayload
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.ui.components.ActionButtonComponent
import de.mctelemetry.core.ui.components.ArgumentInputComponent
import de.mctelemetry.core.ui.components.SelectBoxComponentEntry
import de.mctelemetry.core.ui.datacomponents.ObservationValuePreviewDataComponent
import de.mctelemetry.core.ui.datacomponents.ObservationValueStateDataComponent
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.closeAllCollect
import de.mctelemetry.core.utils.closeConsumeAllRethrow
import de.mctelemetry.core.utils.coroutineDispatcher
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.IStyleBuilder
import de.mctelemetry.core.utils.dsl.components.onHoverShowText
import de.mctelemetry.core.utils.dsl.components.style
import de.mctelemetry.core.utils.globalPosOrThrow
import de.mctelemetry.core.utils.runWithExceptionCleanup
import de.mctelemetry.core.utils.toShortString
import dev.architectury.networking.NetworkManager
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
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.core.GlobalPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.CommonColors
import net.minecraft.world.flag.FeatureFlagSet
import java.util.concurrent.atomic.AtomicReference

@Environment(EnvType.CLIENT)
class ScraperBlockScreen(
    private val globalPos: GlobalPos,
    private val observationSourceContainer: ObservationSourceContainer<*>,
    private val titleComponent: Component
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "scraper-block"
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

    private val globalClosable = mutableListOf<AutoCloseable>()

    init {
        OTelCoreMod.logger.trace("Opened new coroutine scope for {}", this)
        globalClosable.add(observationSourceContainer.subscribeOnStateAdded { _, _ -> rebuild() })
        globalClosable.add(observationSourceContainer.subscribeOnStateRemoved { _, _ -> rebuild() })
    }

    private val observationValuePreviews: Byte2ObjectMap<ObservationValuePreviewDataComponent> =
        Byte2ObjectOpenHashMap()

    private val listClosable = mutableListOf<AutoCloseable>()

    private var observationList: FlowLayout? = null
    private var isDeleteMode = false

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
            val ex1 = listClosable.closeAllCollect()
            globalClosable.closeConsumeAllRethrow(ex1)
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
        isDeleteMode = hasShiftDown()

        rootComponent.childByIdOrThrow<LabelComponent>("title").text(titleComponent)

        val instrumentManagerButton: ButtonComponent = rootComponent.childWidgetByIdOrThrow("instrument-manager")
        instrumentManagerButton.onPress {
            val instrumentManager = ClientInstrumentMetaManager.activeWorldManager ?: return@onPress
            Minecraft.getInstance().setScreen(InstrumentManagerScreen(instrumentManager))
        }

        observationList = rootComponent.childByIdOrThrow("list")
        rebuild()

        observationFlowRef.get()?.let { observationFlow ->
            if (!observationFlow.isCompleted) return@let
            scope.launch {
                acceptObservations(observationFlow.await().value)
            }
        }
    }

    override fun tick() {
        super.tick()

        if (isDeleteMode != hasShiftDown()) {
            isDeleteMode = hasShiftDown()
            rebuild()
        }
    }

    private fun rebuild() {
        val list = observationList ?: return
        list.clearChildren()

        listClosable.closeConsumeAllRethrow()
        listClosable.clear()

        val storedObservations: Byte2ObjectMap<RecordedObservations> =
            Byte2ObjectOpenHashMap<RecordedObservations>().apply {
                for (entry in observationValuePreviews.byte2ObjectEntrySet()) {
                    val value = entry.value.value ?: continue
                    put(entry.byteKey, value)
                }
            }
        observationValuePreviews.clear()

        var i = -1
        for (entry in observationSourceContainer.observationStates.byte2ObjectEntrySet()) {
            i += 1
            val state = entry.value
            val instanceID = entry.byteKey.toUByte()
            val template = model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:scraper-block",
                mapOf()
            )
            template.childByIdOrThrow<LabelComponent>("observation-source-name").text(
                TranslationKeys.ObservationSources[state.source].also {
                    if (minecraft!!.options.advancedItemTooltips)
                        it.withStyle(IStyleBuilder.buildStyle {
                            onHoverShowText {
                                append(state.source.id.location().toString())
                                append("/")
                                append(state.id.toString())
                            }
                        })
                }
            )
            list.child(template)

            listClosable.add(
                ObservationValueStateDataComponent(
                    template.childByIdOrThrow<LabelComponent>("observation-source-state"),
                    state
                )
            )

            observationValuePreviews[instanceID.toByte()] = ObservationValuePreviewDataComponent(
                template.childByIdOrThrow<LabelComponent>("observation-source-value")
            ).also {
                val storedValue = storedObservations.get(instanceID.toByte())
                if (storedValue != null) {
                    it.value = storedValue
                }
            }

            val editButton: ButtonComponent = template.childWidgetByIdOrThrow("observation-source-edit")
            if (isDeleteMode) {
                editButton.message = buildComponent {
                    +"❌"
                    style {
                        color(CommonColors.RED)
                    }
                }
                editButton.onPress {
                    NetworkManager.sendToServer(
                        C2SObservationSourceStateRemovePayload(
                            globalPos,
                            entry.byteKey.toUByte()
                        )
                    )
                }
            } else {
                editButton.onPress {
                    Minecraft.getInstance().setScreen(ScraperBlockScreenDetails(this, globalPos, state))
                }
            }
        }

        val options = observationSourceContainer.observationSources
            .filter { it is IObservationSourceSingleton<*, *, *> || it is IParameterizedObservationSource<*, *> }
            .map {
                SelectBoxComponentEntry(it, TranslationKeys.ObservationSources[it])
            }

        val createButton = ActionButtonComponent(
            TranslationKeys.Ui.addObservations(),
            TranslationKeys.Ui.addObservations(),
            options
        ) { opt ->
            when (val newValue = opt.value) {
                is IObservationSourceSingleton<*, *, *> -> {
                    NetworkManager.sendToServer(C2SObservationSourceStateAddPayload(globalPos, newValue))
                }

                is IParameterizedObservationSource<*, *> -> {
                    ArgumentInputComponent(
                        list,
                        TranslationKeys.Ui.addObservations(),
                        TranslationKeys.Ui.addObservations(),
                        newValue.parameters
                    ) {
                        NetworkManager.sendToServer(
                            C2SObservationSourceStateAddPayload(
                                globalPos,
                                newValue.instanceFromParameters(it)
                            )
                        )
                    }
                }
            }
        }
        list.child(createButton)
    }
}
