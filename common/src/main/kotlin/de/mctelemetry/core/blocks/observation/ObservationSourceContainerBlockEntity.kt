package de.mctelemetry.core.blocks.observation

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.blocks.RedstoneScraperBlock
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus

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
        container.loadStatesFromTag(compoundTag, observationSourceLookup)
    }

    override fun saveAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider) {
        super.saveAdditional(compoundTag, provider)
        container?.saveStatesToTag(compoundTag)
    }

    private fun updateErrorState() {
        val container = container ?: return
        var ok = true
        var error = false
        for (state in container.observationStates.values) {
            when (state.errorState) {
                ObservationSourceState.ErrorState.Ok -> {}
                is ObservationSourceState.ErrorState.Warnings -> ok = false
                is ObservationSourceState.ErrorState.Errors -> error = true
            }
        }
        val isError = blockState.getValue(RedstoneScraperBlock.ERROR)
        if (isError != error) {
            level!!.setBlock(blockPos, blockState.setValue(RedstoneScraperBlock.ERROR, error), 2)
        }
    }

    fun doBlockTick() {
        OTelCoreMod.logger.debug("Ticking {}@{} in {}", this.javaClass.simpleName, blockPos, level)
        updateErrorState()
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
            container.observationStates.values.forEach {
                it.subscribeToDirty(::onDirty)
            }
            if (!level.isClientSide) {
                if(level.isLoaded(blockPos)) {
                    level.scheduleTick(blockPos, blockState.block, 1)
                } else {
                    updateErrorState()
                }
            }
        }
    }

    private fun onDirty(state: ObservationSourceState) {
        val level = level!!
        if (!level.isClientSide) {
            updateErrorState()
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

        fun loadStatesFromTag(
            compoundTag: CompoundTag,
            observationSourceLookup: HolderLookup.RegistryLookup<IObservationSource<*, *>>,
        ) {
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
                val source = observationSourceLookup.getOrThrow(resourceKey).value()

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
                val data = tag.getCompound("data")
                state.loadFromTag(data)
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
