@file:Suppress("unused")

package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.useInstrumentManagerWhenAvailable
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.toNbt
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.component.OTelCoreModComponents
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.observations.model.ObservationSourceStateID
import de.mctelemetry.core.utils.globalPos
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMaps
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet
import it.unimi.dsi.fastutil.bytes.ByteSet
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.SectionPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrElse

abstract class ObservationSourceContainerBlockEntity(
    blockEntityType: BlockEntityType<*>,
    blockPos: BlockPos,
    blockState: BlockState,
) : BlockEntity(blockEntityType, blockPos, blockState) {

    private var _container: BlockEntityObservationSourceContainer? = null
        set(value) {
            val oldValue = field
            if (oldValue === value) return
            if (oldValue != null) {
                runWithExceptionCleanup(cleanup = { field = null }, oldValue::close)
            }
            field = value
        }
    val container: ObservationSourceContainer<ObservationSourceContainerBlockEntity>
        get() = _container ?: throw IllegalStateException("Container has not been initialized yet")
    val containerIfInitialized: ObservationSourceContainer<ObservationSourceContainerBlockEntity>?
        get() = _container

    private var setupRun = false
    private var onLevelCallback: ((Level) -> Unit)? = null

    val observationStates: Byte2ObjectMap<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>>
        get() = (_container ?: throw IllegalStateException("Container has not been initialized yet")).observationStates
    val observationStatesIfInitialized: Byte2ObjectMap<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>>?
        get() = _container?.observationStates

    protected val blockEntityType: BlockEntityType<*>
        get() = super.type

    abstract override fun getType(): BlockEntityType<out ObservationSourceContainerBlockEntity>

    protected open fun findObservationSources(provider: HolderLookup.Provider): List<IObservationSource<*, *>> {
        val sourceLookup = provider.lookupOrThrow(OTelCoreModAPI.ObservationSources)
        val blockKey = blockState.blockHolder.unwrapKey().getOrElse { return emptyList() }
        val matchingTagEntries = sourceLookup.get(TagKey.create(OTelCoreModAPI.ObservationSources, blockKey.location()))
            .getOrElse { return emptyList() }!!
        return matchingTagEntries.map(Holder<IObservationSource<*, *>>::value)
    }

    override fun loadAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider) {
        super.loadAdditional(compoundTag, provider)
        val container = this._container ?: (BlockEntityObservationSourceContainer(
            findObservationSources(provider)
        ).also {
            this._container = it
        })
        val level = level
        if (level == null) {
            require(onLevelCallback == null) { "onLevelCallback already set" }
            val callback = container.loadStatesDelayedFromTag(compoundTag, provider)
            require(onLevelCallback == null) { "onLevelCallback already set" }
            onLevelCallback = callback
        } else if (level.isClientSide) {
            container.loadStatesFromTag(compoundTag, provider, null)
        } else {
            level as ServerLevel
            val manager = level.server.instrumentManager
            if (manager != null) {
                container.loadStatesFromTag(compoundTag, provider, manager)
            } else {
                val callback = container.loadStatesDelayedFromTag(compoundTag, provider)
                callback(level)
            }
        }
    }

    override fun saveAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider) {
        super.saveAdditional(compoundTag, provider)
        _container?.saveStatesToTag(compoundTag)
    }

    override fun getUpdateTag(provider: HolderLookup.Provider): CompoundTag {
        val updateTag: CompoundTag = super.getUpdateTag(provider)
        _container?.saveStatesToTag(updateTag)
        return updateTag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    private fun updateState() {
        if (level?.isClientSide == true) return
        val container = _container ?: return
        container.markInitialized()
        val targetState: ObservationSourceErrorState.Type =
            container.observationStates.values.map { it.errorState.type }
                .fold(ObservationSourceErrorState.Type.NotConfigured) { acc, type ->
                    when (type) {
                        ObservationSourceErrorState.Type.NotConfigured -> acc
                        ObservationSourceErrorState.Type.Ok -> {
                            if (acc == ObservationSourceErrorState.Type.NotConfigured)
                                ObservationSourceErrorState.Type.Ok
                            else
                                acc
                        }

                        ObservationSourceErrorState.Type.Warnings -> {
                            if (acc != ObservationSourceErrorState.Type.Errors)
                                ObservationSourceErrorState.Type.Warnings
                            else
                                ObservationSourceErrorState.Type.Errors
                        }

                        ObservationSourceErrorState.Type.Errors -> ObservationSourceErrorState.Type.Errors
                    }
                }
        val currentState = blockState.getValue(ObservationSourceContainerBlock.ERROR)
        if (currentState != targetState) {
            level!!.setBlock(
                blockPos,
                blockState.setValue(
                    ObservationSourceContainerBlock.ERROR,
                    targetState
                ),
                2
            )
        }
    }

    fun percussiveMaintenance(player: Player? = null) {
        observationStates.values.forEach {
            it.resetErrorState()
        }
        updateState()
        val level = level ?: return
        level.playSound(player, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 7.0f / 8.0f)
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
            val container = this._container ?: (BlockEntityObservationSourceContainer(
                findObservationSources(level.registryAccess()),
            ).also {
                this._container = it
            })
            container.setupCallbacks()
            if (!level.isClientSide) {
                (level as ServerLevel).server.useInstrumentManagerWhenAvailable {
                    container.cascadesUpdates = true
                }
            } else {
                container.cascadesUpdates = false
            }
            val onLevelCallback = this.onLevelCallback
            if (onLevelCallback != null) {
                onLevelCallback(level)
                this.onLevelCallback = null
            }
            if (!level.isClientSide) {
                level as ServerLevel
                if (level.isLoaded(blockPos)) {
                    container.markInitialized()
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

    private fun onDirty(state: ObservationSourceState<in ObservationSourceContainerBlockEntity, *>) {
        generalBlockSync()
    }

    private fun generalBlockSync() {
        setChanged()
        val level = level!!
        if (!level.isClientSide) {
            if (level.isLoaded(blockPos)) {
                level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
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
        val oldContainer = _container
        try {
            oldContainer?.close()
        } finally {
            _container = null
        }
    }

    override fun clearRemoved() {
        super.clearRemoved()
        val level = level
        if (level != null) {
            setup(level)
        }
    }

    override fun applyImplicitComponents(dataComponentInput: DataComponentInput) {
        super.applyImplicitComponents(dataComponentInput)
        val generateSingletonStates: Boolean =
            dataComponentInput.getOrDefault(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), false)
        val container = _container!!
        if (level?.isClientSide == false) {
            if (generateSingletonStates && container.observationStates.isEmpty()) {
                for (source in container.observationSources) {
                    if (source !is IObservationSourceSingleton<*, *, *>) continue
                    container.addObservationSourceState(source)
                }
            }
        }
    }

    protected open inner class BlockEntityObservationSourceContainer(
        observationSources: Iterable<IObservationSource<*, *>>,
    ) : ObservationSourceContainer<ObservationSourceContainerBlockEntity>() {

        override val observationSources: Set<IObservationSource<in ObservationSourceContainerBlockEntity, *>> =
            observationSources.mapNotNullTo(mutableSetOf()) {
                if (!it.sourceContextType.isAssignableFrom(ObservationSourceContainerBlockEntity::class.java)) {
                    return@mapNotNullTo null
                }
                @Suppress("UNCHECKED_CAST") // cast indirectly checked by contextType
                (it as IObservationSource<in ObservationSourceContainerBlockEntity, *>)
            }

        protected var _cascadesUpdates: Boolean = false
        open var cascadesUpdates: Boolean
            get() = _cascadesUpdates
            set(value) {
                _cascadesUpdates = value
                _observationStates.values.forEach {
                    it.cascadeUpdates = value
                }
            }

        private val idCounter = AtomicInteger(-1)
        val initialized: Boolean get() = idCounter.get() >= 0

        fun markInitialized() {
            if (initialized) return
            idCounter.compareAndSet(-1, 0)
            var testId = 0
            while (testId < UByte.MAX_VALUE.toInt() && _observationStates.containsKey(testId.toByte())) {
                val nextId = testId + 1
                if (nextId > UByte.MAX_VALUE.toInt()) {
                    idCounter.set(UByte.MAX_VALUE.toInt())
                    return
                }
                val witnessId = idCounter.compareAndExchange(testId, nextId)
                testId = if (witnessId == testId) {
                    nextId
                } else {
                    witnessId
                }
            }
        }

        protected val _observationStates: Byte2ObjectMap<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>> =
            Byte2ObjectOpenHashMap()

        override val observationStates: Byte2ObjectMap<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>> =
            Byte2ObjectMaps.unmodifiable(_observationStates)

        public override fun setupCallbacks() {
            super.setupCallbacks()
        }

        override fun setupCallback(state: ObservationSourceState<in ObservationSourceContainerBlockEntity, *>) {
            super.setupCallback(state)
            state.subscribeToDirty(this@ObservationSourceContainerBlockEntity::onDirty)
        }

        protected open fun <SC, I : IObservationSourceInstance<SC, *, I>>
                instantiateObservationSource(source: IObservationSource<SC, I>, data: Tag?, context: SC): I {
            return source.fromNbt(data).also {
                it.onLoad(context)
            }
        }

        protected fun getNextId(): Byte {
            if (idCounter.get() < 0) throw IllegalStateException("Not initialized yet")
            // Race condition does not matter because idCounter never transitions back to -1.
            return idCounter.getAndUpdate { (it + 1) % 256 }.toByte()
        }

        protected open fun <SC, I : IObservationSourceInstance<SC, *, I>> makeNewSourceState(
            instance: I,
            id: ObservationSourceStateID
        ): ObservationSourceState<SC, I> {
            return runWithExceptionCleanup(instance::close) {
                ObservationSourceState(instance, id)
            }
        }

        final override fun addObservationSourceState(
            source: IObservationSource<in ObservationSourceContainerBlockEntity, *>,
            data: Tag?
        ): ObservationSourceState<in ObservationSourceContainerBlockEntity, *> {
            @Suppress("UNCHECKED_CAST")
            val instance = instantiateObservationSource(
                source as IObservationSource<ObservationSourceContainerBlockEntity, *>,
                data,
                this@ObservationSourceContainerBlockEntity
            )
            return addObservationSourceState(instance)
        }

        override fun addObservationSourceState(
            instance: IObservationSourceInstance<in ObservationSourceContainerBlockEntity, *, *>
        ): ObservationSourceState<in ObservationSourceContainerBlockEntity, *> {
            var result: ObservationSourceState<in ObservationSourceContainerBlockEntity, *>? = null
            var nextId: Byte
            val startId: Byte
            runWithExceptionCleanup(instance::close) {
                if (_observationStates.size >= UByte.MAX_VALUE.toInt()) {
                    throw IllegalArgumentException("Cannot add more than 256 observations to one ObservationSourceContainer")
                }
                nextId = getNextId()
                startId = nextId
            }
            result =
                run { // same value will already be stored in result when this assignment happens, this just helps the type checker
                    do {
                        if (!_observationStates.containsKey(nextId)) {
                            synchronized(_observationStates) {
                                _observationStates.computeIfAbsent(nextId) { id: Byte ->
                                    makeNewSourceState(
                                        instance,
                                        id.toUByte()
                                    ).also {
                                        result = it
                                    }
                                }
                            }
                            if (result != null) {
                                return@run result
                            }
                        }
                        runWithExceptionCleanup(instance::close) {
                            nextId = getNextId()
                        }
                    } while (nextId != startId)
                    instance.close()
                    throw IllegalArgumentException("Cannot add more than 256 observations to one ObservationSourceContainer")
                }
            setupCallback(result)
            triggerStateAdded(result)
            result.cascadeUpdates = _cascadesUpdates
            return result
        }

        override fun removeObservationSourceState(id: ObservationSourceStateID): Boolean {
            val removed = synchronized(_observationStates) {
                _observationStates.remove(id.toByte())
            } ?: return false
            try {
                removed.close()
                triggerStateRemoved(removed)
            } finally {
                generalBlockSync()
            }
            return true
        }

        override val context: ObservationSourceContainerBlockEntity
            get() = this@ObservationSourceContainerBlockEntity

        override val instrumentManager: IInstrumentManager
            get() {
                val level = level
                    ?: throw NullPointerException("BlockEntity not assigned to a level: ${this@ObservationSourceContainerBlockEntity}")
                if (level.isClientSide) {
                    return IClientWorldInstrumentManager.clientWorldInstrumentManager
                        ?: throw NullPointerException("Instrument manager not initialized for client")
                }
                val server = level.server ?: throw NullPointerException("Level not bound to a server: $level")
                return server.instrumentManager
                    ?: throw NullPointerException("Instrument manager not available for server: $server")
            }

        private fun <I : IObservationSourceInstance<in ObservationSourceContainerBlockEntity, *, I>> mergeSourceInstanceIntoExistingState(
            state: ObservationSourceState<*, *>,
            source: IObservationSource<*, I>,
            data: Tag?
        ) {
            if (state.source !== source) {
                throw IllegalArgumentException("Cannot merge new observation source instance for source $source into existing state for source ${state.source} (id: ${state.id}")
            }
            @Suppress("UNCHECKED_CAST") // cast is guaranteed by equality of `source` and `state.source`
            state as ObservationSourceState<*, I>
            @Suppress("UNCHECKED_CAST")
            // could be checked statically by providing SC as function type parameter, but SC is not known at call site.
            val instance = instantiateObservationSource(
                source as IObservationSource<
                        ObservationSourceContainerBlockEntity,
                        IObservationSourceInstance<ObservationSourceContainerBlockEntity, *, *>
                        >,
                data,
                this@ObservationSourceContainerBlockEntity,
            ) as I
            state.instance = instance
        }

        private inline fun <T> loadTagsAndApplyToState(
            compoundTag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
            removeMissing: Boolean = true,
            block: (source: ObservationSourceState<in ObservationSourceContainerBlockEntity, *>, data: CompoundTag, isNew: Boolean) -> T,
        ): Map<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>, T> {
            contract {
                callsInPlace(block, InvocationKind.UNKNOWN)
            }
            return buildMap {
                synchronized(_observationStates) {

                    val observationsTag = compoundTag.getList("observations", Tag.TAG_COMPOUND.toInt())
                    for ((observationIndex, tag) in observationsTag.withIndex()) {
                        tag as CompoundTag
                        val sourceIdString = tag.getString("id")
                        if (sourceIdString.isBlank()) {
                            OTelCoreMod.logger.warn(
                                "Empty observation-source-id during loading of nbt for {}@{}#{}",
                                this@ObservationSourceContainerBlockEntity,
                                this@ObservationSourceContainerBlockEntity.globalPos,
                                observationIndex,
                            )
                            continue
                        }
                        val resourceKey =
                            ResourceKey.create(
                                OTelCoreModAPI.ObservationSources,
                                ResourceLocation.parse(sourceIdString)
                            )
                        val source = holderLookupProvider.lookupOrThrow(OTelCoreModAPI.ObservationSources)
                            .getOrThrow(resourceKey)
                            .value()

                        if (source !in observationSources) {
                            OTelCoreMod.logger.warn(
                                "Observation-source-type {} is not enabled for {}@{}#{}",
                                resourceKey,
                                this@ObservationSourceContainerBlockEntity,
                                this@ObservationSourceContainerBlockEntity.globalPos,
                                observationIndex,
                            )
                            continue
                        }
                        assert(source.sourceContextType.isAssignableFrom(ObservationSourceContainerBlockEntity::class.java))
                        @Suppress("UNCHECKED_CAST")
                        // cast indirectly checked by being element of observationSources
                        source as IObservationSource<in ObservationSourceContainerBlockEntity, *>

                        val instanceId: Byte = if (tag.contains("index", Tag.TAG_BYTE.toInt()))
                            tag.getByte("index")
                        else
                            throw IllegalArgumentException("Received no instanceId/index for ${this@ObservationSourceContainerBlockEntity}@${this@ObservationSourceContainerBlockEntity.globalPos}#$observationIndex")
                        var isNew = false
                        val state = _observationStates.compute(instanceId) { instanceId, old ->
                            if (old != null) {
                                if (source.javaClass !== old.source.javaClass || source !is IObservationSourceSingleton<*, *, *>) {
                                    mergeSourceInstanceIntoExistingState(old, source, tag.get("params"))
                                }
                                old
                            } else {
                                isNew = true
                                val sourceInstance = instantiateObservationSource(
                                    source, tag.get("params"),
                                    this@ObservationSourceContainerBlockEntity
                                )
                                makeNewSourceState(sourceInstance, instanceId.toUByte()).also {
                                    runWithExceptionCleanup(it::close) {
                                        it.cascadeUpdates = _cascadesUpdates
                                    }
                                }
                            }
                        }
                        runWithExceptionCleanup({ _observationStates.remove(instanceId)?.close() }) {
                            put(state, block(state, tag.getCompound("data"), isNew))
                        }
                        if (isNew) {
                            triggerStateAdded(state)
                        }
                    }
                    if (removeMissing) {
                        val missingIdsSet: ByteSet = ByteOpenHashSet(_observationStates.keys)
                        for (state in this@buildMap.keys) {
                            missingIdsSet.remove(state.id.toByte())
                        }
                        if (missingIdsSet.isNotEmpty()) {
                            val iter = missingIdsSet.iterator()
                            var exceptionAccumulator: Exception? = null
                            while (iter.hasNext()) {
                                val id = iter.nextByte()
                                val oldValue = _observationStates.remove(id)
                                try {
                                    oldValue?.close()
                                    triggerStateRemoved(oldValue)
                                } catch (ex: Exception) {
                                    exceptionAccumulator += ex
                                }
                            }
                            if (exceptionAccumulator != null) {
                                throw exceptionAccumulator
                            }
                        }
                    }
                    markInitialized()
                }
            }
        }

        fun loadStatesFromTag(
            compoundTag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
            instrumentManager: IMutableInstrumentManager?,
            removeMissing: Boolean = true,
        ) {
            loadTagsAndApplyToState(
                compoundTag,
                holderLookupProvider,
                removeMissing = removeMissing,
            ) { state, dataTag, isNew ->
                if (isNew) {
                    setupCallback(state)
                    state.cascadeUpdates = _cascadesUpdates
                }
                state.loadFromTag(dataTag, holderLookupProvider, instrumentManager)
            }
        }


        fun loadStatesDelayedFromTag(
            compoundTag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
            removeMissing: Boolean = true,
        ): (Level) -> Unit {
            var pendingNewStates: MutableList<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>>? =
                null
            val delayedMap = loadTagsAndApplyToState(
                compoundTag,
                holderLookupProvider,
                removeMissing = removeMissing,
            ) { state, dataTag, isNew ->
                if (isNew) {
                    if (pendingNewStates == null)
                        pendingNewStates = mutableListOf(state)
                    else
                        pendingNewStates.add(state)
                }
                state.loadDelayedFromTag(dataTag, holderLookupProvider)
            }

            return { level ->
                if (pendingNewStates != null) {
                    for (state in pendingNewStates) {
                        setupCallback(state)
                        state.cascadeUpdates = _cascadesUpdates
                    }
                }
                if (level.isClientSide) {
                    for (delayedCallback in delayedMap.values) {
                        delayedCallback.invoke(null)
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
                    stateTag.putByte("index", state.id.toByte())

                    val instance = state.instance
                    stateTag.putString("id", instance.source.id.location().toString())
                    val paramsTag = instance.toNbt()
                    if (paramsTag != null) {
                        stateTag.put("params", paramsTag)
                    }

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
