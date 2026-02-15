@file:Suppress("unused")

package de.mctelemetry.core.blocks.entities

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.ListBuilder
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
import de.mctelemetry.core.api.observations.encode
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.component.OTelCoreModComponents
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.observations.model.ObservationSourceStateID
import de.mctelemetry.core.utils.addErrorTo
import de.mctelemetry.core.utils.forEachMapEntry
import de.mctelemetry.core.utils.get
import de.mctelemetry.core.utils.getNumberValue
import de.mctelemetry.core.utils.getParsedValue
import de.mctelemetry.core.utils.getStream
import de.mctelemetry.core.utils.globalPos
import de.mctelemetry.core.utils.isEmptyMap
import de.mctelemetry.core.utils.mergeErrorMessages
import de.mctelemetry.core.utils.resultOrPartialOrElse
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.resultOrElse
import de.mctelemetry.core.utils.resultOrNull
import de.mctelemetry.core.utils.runWithExceptionCleanup
import de.mctelemetry.core.utils.withEntry
import dev.architectury.platform.Platform
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
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.RegistryOps
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
import java.util.function.Supplier
import kotlin.collections.forEach
import kotlin.collections.mapNotNullTo
import kotlin.collections.mutableSetOf
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrElse
import kotlin.streams.asSequence

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
        val ops: DynamicOps<Tag> = RegistryOps.create(NbtOps.INSTANCE, provider)
        context(ops) {
            if (level == null) {
                require(onLevelCallback == null) { "onLevelCallback already set" }
                val callback = container.loadStatesDelayed(compoundTag)
                require(onLevelCallback == null) { "onLevelCallback already set" }
                onLevelCallback = callback.resultOrPartialOrElse(
                    onError = {
                        OTelCoreMod.logger.error("Error(s) during pre-loading of ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                    },
                    fallback = { it.partialOrThrow },
                )
            } else if (level.isClientSide) {
                container.loadStatesFromTag(compoundTag, null).ifError {
                    OTelCoreMod.logger.error("Error(s) during client-loading of ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                }.partialOrThrow
            } else {
                level as ServerLevel
                val manager = level.server.instrumentManager
                if (manager != null) {
                    container.loadStatesFromTag(compoundTag, manager).ifError {
                        OTelCoreMod.logger.error("Error(s) during server-loading of ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                    }
                } else {
                    val callback = container.loadStatesDelayed(compoundTag).resultOrPartialOrElse(
                        onError = {
                            OTelCoreMod.logger.error("Error(s) during server-pre-loading of ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                        },
                        fallback = { it.partialOrThrow },
                    )
                    callback(level)
                }
            }
        }
    }

    override fun saveAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider) {
        super.saveAdditional(compoundTag, provider)
        val ops = RegistryOps.create(NbtOps.INSTANCE, provider)
        val container = _container ?: return
        context(ops) {
            val result = container.saveStates(compoundTag).resultOrPartialOrElse(
                onError = {
                    OTelCoreMod.logger.error("Error(s) during saving of ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                },
                fallback = { it.partialOrThrow },
            )
            result.forEachMapEntry { key, value ->
                compoundTag.put(key.asString, value)
            }
        }
        if (Platform.isDevelopmentEnvironment()) {
            fun visit(tag: Tag?, path: String) {
                when (tag?.id) {
                    Tag.TAG_COMPOUND -> {
                        tag as CompoundTag
                        tag.allKeys.forEach { key ->
                            visit(tag[key], "$path.$key")
                        }
                    }

                    Tag.TAG_LIST -> {
                        (tag as ListTag).forEachIndexed { index, subtag ->
                            visit(subtag, "$path[$index]")
                        }
                    }

                    null, Tag.TAG_END -> {
                        throw IllegalArgumentException("Encountered illegal tag $tag at $path")
                    }

                    else -> {}
                }
            }
            visit(compoundTag, "<root>")
        }
    }

    override fun getUpdateTag(provider: HolderLookup.Provider): CompoundTag {
        val updateTag: CompoundTag = super.getUpdateTag(provider)
        val ops = RegistryOps.create(NbtOps.INSTANCE, provider)
        val container = _container ?: return updateTag
        return context(ops) {
            val result: Tag = container.saveStates().resultOrPartialOrElse(
                onError = {
                    OTelCoreMod.logger.error("Error(s) during update-saving of ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                },
                fallback = { it.partialOrThrow },
            )
            result as CompoundTag
        }
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
                val chunkX = SectionPos.blockToSectionCoord(blockPos.x)
                val chunkZ = SectionPos.blockToSectionCoord(blockPos.z)
                val chunk = level.chunkSource.getChunkNow(chunkX, chunkZ)
                if (chunk != null && chunk.loaded) {
                    level.scheduleTick(blockPos, blockState.block, 1)
                }
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
            oldContainer?.close(silent = true)
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
                    container.addObservationSourceState(source as IObservationSourceSingleton<in ObservationSourceContainerBlockEntity, *, *>)
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

        context(ops: DynamicOps<T>)
        protected open fun <T, SC, I : IObservationSourceInstance<SC, *, I>>
                instantiateObservationSource(source: IObservationSource<SC, I>, data: T?): DataResult<I> {
            return source.codec.parse(ops, data ?: ops.empty())
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
            return ObservationSourceState(instance, id)
        }

        context(ops: DynamicOps<T>)
        final override fun <T> addObservationSourceState(
            source: IObservationSource<in ObservationSourceContainerBlockEntity, *>,
            data: T?
        ): ObservationSourceState<in ObservationSourceContainerBlockEntity, *> {
            @Suppress("UNCHECKED_CAST")
            val instance = instantiateObservationSource(
                source as IObservationSource<ObservationSourceContainerBlockEntity, *>,
                data
            ).resultOrPartialOrElse(
                onError = {
                    OTelCoreMod.logger.error("Error(s) during add-loading of $source for ${this@ObservationSourceContainerBlockEntity}@$blockPos: ${it.message()}")
                },
                fallback = { it.partialOrThrow })
            return addObservationSourceState(instance)
        }

        override fun addObservationSourceState(
            instance: IObservationSourceInstance<in ObservationSourceContainerBlockEntity, *, *>
        ): ObservationSourceState<in ObservationSourceContainerBlockEntity, *> {
            if (_observationStates.size >= UByte.MAX_VALUE.toInt()) {
                throw IllegalArgumentException("Cannot add more than 256 observations to one ObservationSourceContainer")
            }
            var result: ObservationSourceState<in ObservationSourceContainerBlockEntity, *>? = null
            var nextId: Byte = getNextId()
            val startId: Byte = nextId
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
                        nextId = getNextId()
                    } while (nextId != startId)
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

        context(ops: DynamicOps<T>)
        private fun <T, I : IObservationSourceInstance<in ObservationSourceContainerBlockEntity, *, I>> mergeSourceInstanceIntoExistingState(
            state: ObservationSourceState<*, *>,
            source: IObservationSource<*, I>,
            data: T?
        ): DataResult<Unit> {
            if (state.source !== source) {
                return DataResult.error {
                    "Cannot merge new observation source instance for source $source into existing state for source ${state.source} (id: ${state.id}"
                }
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
                data
            ) as DataResult<I>
            if (instance.hasResultOrPartial()) {
                state.instance = instance.partialOrThrow // should never occur because of earlier check
            }
            // Unit will not be returned when no result or partial is available due to behavior of DataResult.Error.map
            return instance.map { } // maps successful or partial instance to Unit
        }

        context(ops: DynamicOps<T>)
        private inline fun <T, R> loadTagsAndApplyToState(
            input: T,
            removeMissing: Boolean = true,
            crossinline block: context(DynamicOps<T>) (state: ObservationSourceState<in ObservationSourceContainerBlockEntity, *>, data: T?, isNew: Boolean) -> DataResult<R>,
        ): DataResult<Map<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>, DataResult<R>>> {
            contract {
                callsInPlace(block, InvocationKind.UNKNOWN)
            }
            val errors: MutableList<Supplier<String>> = mutableListOf()
            val result = buildMap {
                synchronized(_observationStates) {
                    val observationsStream = input.getStream("observations").resultOrPartialOrElse(
                        onError = { it.addErrorTo(errors) },
                        fallback = { return DataResult.error(mergeErrorMessages(errors)) }
                    )

                    observationsStream.asSequence().forEachIndexed { observationIndex, observationElement ->
                        val source = observationElement
                            .getParsedValue("id", IObservationSource.CODEC)
                            .resultOrPartialOrElse(
                                onError = { it.addErrorTo(errors) },
                                fallback = { return@forEachIndexed }
                            )
                        if (source !in observationSources) {
                            errors.add(Supplier {
                                "Observation-source-type $source is not enabled for ${this@ObservationSourceContainerBlockEntity}@${this@ObservationSourceContainerBlockEntity.globalPos}#$observationIndex"
                            })
                            return@forEachIndexed
                        }
                        assert(source.sourceContextType.isAssignableFrom(ObservationSourceContainerBlockEntity::class.java))
                        @Suppress("UNCHECKED_CAST")
                        // cast indirectly checked by being element of observationSources
                        source as IObservationSource<in ObservationSourceContainerBlockEntity, *>

                        val instanceId: Byte = observationElement.getNumberValue("index").resultOrElse {
                            it.addErrorTo(errors)
                            return@forEachIndexed
                        }.toByte()
                        var isNew = false
                        val state = _observationStates.compute(instanceId) { instanceId, old ->
                            if (old != null) {
                                if (source.javaClass !== old.source.javaClass || source !is IObservationSourceSingleton<*, *, *>) {
                                    val mergeResultData = mergeSourceInstanceIntoExistingState(
                                        old,
                                        source,
                                        observationElement["params"].resultOrNull()
                                    )
                                    mergeResultData.addErrorTo(errors)
                                }
                                old
                            } else {
                                isNew = true
                                val instantiationResult =
                                    instantiateObservationSource(source, observationElement["params"].resultOrNull())
                                instantiationResult.addErrorTo(errors)
                                val sourceInstance = instantiationResult.resultOrPartialOrElse(
                                    onError = { it.addErrorTo(errors) },
                                    fallback = { return@compute null },
                                )
                                makeNewSourceState(sourceInstance, instanceId.toUByte()).also {
                                    it.cascadeUpdates = _cascadesUpdates
                                }
                            }
                        }
                        if (state == null) return@forEachIndexed
                        runWithExceptionCleanup({ _observationStates.remove(instanceId)?.close() }) {
                            val blockResult = block(state, observationElement["data"].resultOrNull(), isNew)
                            blockResult.addErrorTo(errors)
                            put(state, blockResult)
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
            return if (errors.isEmpty())
                DataResult.success(result)
            else
                DataResult.error(mergeErrorMessages(errors), result)
        }

        context(ops: DynamicOps<T>)
        fun <T> loadStatesFromTag(
            input: T,
            instrumentManager: IMutableInstrumentManager?,
            removeMissing: Boolean = true,
        ): DataResult<Unit> {
            return loadTagsAndApplyToState(
                input,
                removeMissing = removeMissing,
            ) { state, data, isNew ->
                if (isNew) {
                    setupCallback(state)
                    state.cascadeUpdates = _cascadesUpdates
                }
                state.applyDecode(data ?: ops.emptyMap(), instrumentManager)
            }.map { }
        }


        context(ops: DynamicOps<T>)
        fun <T> loadStatesDelayed(
            input: T,
            removeMissing: Boolean = true,
        ): DataResult<(Level) -> Unit> {
            var pendingNewStates: MutableList<ObservationSourceState<in ObservationSourceContainerBlockEntity, *>>? =
                null
            val delayedResultMap = loadTagsAndApplyToState(
                input,
                removeMissing = removeMissing,
            ) { state, data, isNew ->
                if (isNew) {
                    if (pendingNewStates == null)
                        pendingNewStates = mutableListOf(state)
                    else
                        pendingNewStates.add(state)
                }
                state.applyDelayedDecode(data ?: ops.emptyMap())
            }

            return delayedResultMap.map { delayedMap ->
                { level: Level ->
                    if (pendingNewStates != null) {
                        for (state in pendingNewStates) {
                            setupCallback(state)
                            state.cascadeUpdates = _cascadesUpdates
                        }
                    }
                    if (level.isClientSide) {
                        for (delayedCallback in delayedMap.values) {
                            delayedCallback.resultOrPartialOrElse { continue }.invoke(null)
                        }
                    } else {
                        (level as ServerLevel).server.useInstrumentManagerWhenAvailable { manager ->
                            for (delayedCallback in delayedMap.values) {
                                delayedCallback.resultOrPartialOrElse { continue }.invoke(manager)
                            }
                        }
                    }
                }
            }
        }

        context(ops: DynamicOps<T>)
        fun <T> saveStates(
            prefix: T = ops.empty()
        ): DataResult<T> {
            val observationListBuilder: ListBuilder<T> = ops.listBuilder()
            for (state in observationStates.values) {
                observationListBuilder.add(ops.mapBuilder().also { entryBuilder ->
                    entryBuilder.add("index", state.id.toByte(), Codec.BYTE)

                    val instance = state.instance

                    entryBuilder.add("id", instance.source, IObservationSource.CODEC)

                    val paramsResult = instance.encode()
                    if (paramsResult.isError || paramsResult.orThrow != ops.empty()) {
                        entryBuilder.add("params", paramsResult)
                    }

                    val dataResult = state.encode()
                    var defaultDataResult = false
                    if (dataResult.isSuccess) {
                        val result = dataResult.orThrow
                        defaultDataResult = (result == ops.empty()) || result.isEmptyMap()
                    }
                    if (!defaultDataResult) {
                        entryBuilder.add("data", dataResult)
                    }
                }.build(ops.emptyMap()))
            }
            return observationListBuilder.build(ops.emptyList()).map {
                prefix.withEntry("observations", it)
            }
        }
    }
}
