package de.mctelemetry.core.blocks.observation

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.useInstrumentManagerWhenAvailable
import de.mctelemetry.core.blocks.RedstoneScraperBlock
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.SectionPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ObservationSourceContainerBlockEntity(
    blockPos: BlockPos,
    blockState: BlockState,
) : BlockEntity(OTelCoreModBlockEntityTypes.OBSERVATION_SOURCE_CONTAINER_BLOCK_ENTITY.get(), blockPos, blockState) {

    private var container: BlockEntityObservationSourceContainer? = null
        set(value) {
            val oldValue = field
            if (oldValue === value) return
            if (oldValue != null) {
                runWithExceptionCleanup(cleanup = { field = null }, oldValue::close)
            }
            field = value
        }

    private var setupRun = false
    private var onLevelCallback: ((Level) -> Unit)? = null

    internal val observationStates: Map<IObservationSource<in ObservationSourceContainerBlockEntity, *>, ObservationSourceState>?
        get() = container?.observationStates

    override fun getType(): BlockEntityType<out ObservationSourceContainerBlockEntity> {
        @Suppress("UNCHECKED_CAST") // known value from constructor
        return super.type as BlockEntityType<ObservationSourceContainerBlockEntity>
    }

    override fun loadAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider) {
        super.loadAdditional(compoundTag, provider)
        val observationSourceLookup = provider.lookupOrThrow(OTelCoreModAPI.ObservationSources)
        val container = this.container ?: (BlockEntityObservationSourceContainer(
            observationSourceLookup
                .listElements()
                .map(Holder<IObservationSource<*, *>>::value)
                .toList()
        ).also {
            this.container = it
        })
        val callback = container.loadStatesDelayedFromTag(compoundTag, provider)
        val level = level
        if (level != null) {
            callback(level)
        } else {
            require(onLevelCallback == null) { "onLevelCallback already set" }
            onLevelCallback = callback
        }
    }

    override fun saveAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider) {
        super.saveAdditional(compoundTag, provider)
        container?.saveStatesToTag(compoundTag)
    }

    private fun updateState() {
        val container = container ?: return
        var targetState: ObservationSourceState.ErrorState.Type? = null
        for (state in container.observationStates.values) {
            targetState = when (state.errorState) {
                ObservationSourceState.ErrorState.Ok -> {
                    targetState ?: ObservationSourceState.ErrorState.Type.Ok
                }
                is ObservationSourceState.ErrorState.Warnings -> {
                    if (state.errorState.warnings.singleOrNull() === ObservationSourceState.ErrorState.notConfiguredWarning)
                        continue
                    if (targetState == ObservationSourceState.ErrorState.Type.Errors || targetState == null)
                        continue
                    ObservationSourceState.ErrorState.Type.Warnings
                }
                is ObservationSourceState.ErrorState.Errors -> {
                    targetState = ObservationSourceState.ErrorState.Type.Errors
                    break
                }
            }
        }
        targetState = targetState ?: ObservationSourceState.ErrorState.Type.Warnings
        val currentState = blockState.getValue(RedstoneScraperBlock.ERROR)
        if (currentState != targetState) {
            level!!.setBlock(
                blockPos,
                blockState.setValue(
                    RedstoneScraperBlock.ERROR,
                    targetState
                ),
                2
            )
        }
    }

    fun doBlockTick() {
        OTelCoreMod.logger.trace("Ticking {}@{} in {}", this.javaClass.simpleName, blockPos, level)
        updateState()
    }

    private fun setup(level: Level) {
        if (setupRun) return
        setupRun = true
        runWithExceptionCleanup(cleanup = {
            setupRun = false
        }) {
            val container = this.container ?: (BlockEntityObservationSourceContainer(
                level.registryAccess()
                    .registryOrThrow(OTelCoreModAPI.ObservationSources),
            ).also {
                this.container = it
            })
            container.setup()
            container.setCascadeUpdates(!level.isClientSide)
            val onLevelCallback = this.onLevelCallback
            if (onLevelCallback != null) {
                onLevelCallback(level)
                this.onLevelCallback = null
            }
            container.observationStates.values.forEach {
                it.subscribeToDirty(::onDirty)
            }
            if (!level.isClientSide) {
                level as ServerLevel
                if (level.isLoaded(blockPos)) {
                    val chunkX = SectionPos.blockToSectionCoord(blockPos.x)
                    val chunkZ = SectionPos.blockToSectionCoord(blockPos.z)
                    val chunk = level.chunkSource.getChunkNow(chunkX, chunkZ)
                    if (chunk == null) {
                        OTelCoreMod.logger.trace("Chunk is not available right now, skipping setup: {}", blockPos)
                    } else if (!chunk.loaded) {
                        OTelCoreMod.logger.trace("Chunk is unloaded, awaiting platform-dependant loading: {}", blockPos)
                    } else {
                        OTelCoreMod.logger.trace("Chunk is loaded, scheduling tick now: {}", blockPos)
                        level.scheduleTick(blockPos, blockState.block, 1)
                    }
                }
            }
        }
    }

    private fun onDirty(state: ObservationSourceState) {
        setChanged()
        val level = level!!
        if (!level.isClientSide) {
            if (level.isLoaded(blockPos)) {
                level.scheduleTick(blockPos, blockState.block, 1)
            } else {
                updateState()
            }
        }
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        if (!isRemoved)
            setup(level)
    }

    override fun setRemoved() {
        super.setRemoved()
        val oldContainer = container
        try {
            if (oldContainer != null) {
                for (state in oldContainer.observationStates.values) {
                    state.unsubscribeFromDirty(::onDirty)
                }
            }
        } finally {
            container = null
        }
    }

    override fun clearRemoved() {
        super.clearRemoved()
        val level = level
        if (level != null) {
            setup(level)
        }
    }

    private inner class BlockEntityObservationSourceContainer(
        observationSources: Iterable<IObservationSource<*, *>>,
    ) : ObservationSourceContainer<ObservationSourceContainerBlockEntity>() {

        override val observationStates: Map<IObservationSource<in ObservationSourceContainerBlockEntity, *>, ObservationSourceState> by lazy {
            observationSources.mapNotNull {
                if (!it.contextType.isAssignableFrom(ObservationSourceContainerBlockEntity::class.java))
                    return@mapNotNull null
                @Suppress("UNCHECKED_CAST") // cast indirectly checked by contextType
                it as IObservationSource<in ObservationSourceContainerBlockEntity, *>
            }.associateWith { ObservationSourceState(it) }
        }

        @Suppress("RedundantVisibilityModifier") // not actually redundant because of visibility widening
        public override fun setup() {
            super.setup()
        }

        fun setCascadeUpdates(value: Boolean = true) {
            observationStates.values.forEach { it.cascadeUpdates = value }
        }

        override val context: ObservationSourceContainerBlockEntity
            get() = this@ObservationSourceContainerBlockEntity

        override val instrumentManager: IInstrumentManager
            get() {
                val level = level
                    ?: throw NullPointerException("BlockEntity not assigned to a level: ${this@ObservationSourceContainerBlockEntity}")
                val server = level.server ?: throw NullPointerException("Level not bound to a server: $level")
                return server.instrumentManager
                    ?: throw NullPointerException("Instrument manager not available for server: $server")
            }

        @OptIn(ExperimentalContracts::class)
        private inline fun <T> loadAndApplyToStateTags(
            compoundTag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
            block: (ObservationSourceState, CompoundTag) -> T,
        ): Map<ObservationSourceState, T> {
            contract {
                callsInPlace(block, InvocationKind.UNKNOWN)
            }
            return buildMap {
                val observationsTag = compoundTag.getList("observations", Tag.TAG_COMPOUND.toInt())
                for (tag in observationsTag) {
                    tag as CompoundTag
                    val idString = tag.getString("id")
                    if (idString.isBlank()) {
                        OTelCoreMod.logger.warn("Empty observation-source-id during loading of nbt for {}", this)
                        continue
                    }
                    val resourceKey =
                        ResourceKey.create(OTelCoreModAPI.ObservationSources, ResourceLocation.parse(idString))
                    val source = holderLookupProvider.lookupOrThrow(OTelCoreModAPI.ObservationSources)
                        .getOrThrow(resourceKey)
                        .value()

                    val state = observationStates.getOrElse(
                        @Suppress("UNCHECKED_CAST") // actual generic type does not matter because it is only used as lookup key
                        (source as IObservationSource<in ObservationSourceContainerBlockEntity, *>)
                    ) {
                        OTelCoreMod.logger.warn(
                            "Could not find state for observation-source-id {} in {}",
                            resourceKey,
                            this
                        )
                        continue
                    }
                    put(state, block(state, tag))
                }
            }
        }

        fun loadStatesFromTag(
            compoundTag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
            instrumentManager: IInstrumentManager,
        ) {
            loadAndApplyToStateTags(compoundTag, holderLookupProvider) { state, tag ->
                val data = tag.getCompound("data")
                state.loadFromTag(data, holderLookupProvider, instrumentManager)
            }
        }


        fun loadStatesDelayedFromTag(
            compoundTag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
        ): (Level) -> Unit {
            val delayedMap = loadAndApplyToStateTags(compoundTag, holderLookupProvider) { state, tag ->
                val data = tag.getCompound("data")
                state.loadDelayedFromTag(data, holderLookupProvider)
            }

            return { level ->
                if (level.isClientSide) {
                    for (delayedCallback in delayedMap.values) {
                        delayedCallback.invoke(IInstrumentManager.ReadonlyEmpty)
                    }
                } else {
                    (level as ServerLevel).server.useInstrumentManagerWhenAvailable { manager ->
                        for (delayedCallback in delayedMap.values) {
                            delayedCallback.invoke(manager)
                        }
                    }
                }
            }
        }

        fun saveStatesToTag(compoundTag: CompoundTag) {
            val observationsListTag = ListTag()
            for (state in observationStates.values) {
                observationsListTag.add(CompoundTag().also { stateTag ->
                    stateTag.putString("id", state.source.id.location().toString())
                    val dataTag = CompoundTag()
                    state.saveToTag(dataTag)
                    if (!dataTag.isEmpty) {
                        stateTag.put("data", dataTag)
                    }
                })
            }
            compoundTag.put("observations", observationsListTag)
        }
    }
}
