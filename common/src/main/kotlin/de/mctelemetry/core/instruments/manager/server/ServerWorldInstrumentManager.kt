package de.mctelemetry.core.instruments.manager.server

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.IWorldInstrumentRegistration
import de.mctelemetry.core.api.instruments.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IWorldMutableInstrumentRegistration
import de.mctelemetry.core.instruments.manager.GameInstrumentManager
import de.mctelemetry.core.instruments.manager.InstrumentManagerBase
import de.mctelemetry.core.instruments.manager.InstrumentManagerBaseRegistrationUnion
import de.mctelemetry.core.persistence.DirtyCallbackMutableMap
import de.mctelemetry.core.persistence.SavedDataConcurrentMap
import de.mctelemetry.core.utils.Union2
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.metrics.Meter
import net.minecraft.core.HolderGetter
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.storage.DimensionDataStorage
import java.lang.AutoCloseable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class ServerWorldInstrumentManager private constructor(
    meter: Meter,
    override val gameInstruments: GameInstrumentManager,
    server: MinecraftServer,
    localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion>,
) : InstrumentManagerBase.Child<ServerWorldInstrumentManager.WorldGaugeInstrumentBuilder>(
    meter,
    gameInstruments,
    localInstruments,
), IServerWorldInstrumentManager, AutoCloseable {

    constructor(meter: Meter, gameInstruments: GameInstrumentManager, server: MinecraftServer) : this(
        meter,
        gameInstruments,
        server,
        ConcurrentHashMap()
    )

    private val server: WeakReference<MinecraftServer> = WeakReference(server)


    override fun findLocal(name: String): IWorldInstrumentRegistration? {
        return super<Child>.findLocal(name) as IWorldInstrumentRegistration?
    }

    override fun findLocal(pattern: Regex?): Sequence<IWorldInstrumentRegistration> {
        return super<Child>.findLocal(pattern).map { it as IWorldInstrumentRegistration }
    }

    companion object {

        private val WORLDMETRICS_KEY: String = "${OTelCoreMod.MOD_ID}_worldmetrics"

        fun withPersistentStorage(
            meter: Meter,
            gameInstruments: GameInstrumentManager,
            server: MinecraftServer,
            dataStorage: DimensionDataStorage = server.getLevel(ServerLevel.OVERWORLD)!!.dataStorage,
        ): ServerWorldInstrumentManager {
            val saveData = dataStorage.computeIfAbsent(WorldInstrumentSavedData.Factory, WORLDMETRICS_KEY)!!
            val manager = ServerWorldInstrumentManager(meter, gameInstruments, server, saveData)
            runWithExceptionCleanup(manager::close) {
                for (registration in manager.localInstruments.values) {
                    val registrationValue = registration.value
                    manager.triggerOwnInstrumentAdded(registrationValue, IInstrumentAvailabilityCallback.Phase.PRE)
                    val otelRegistration = meter.gaugeBuilder(registrationValue.name)
                        .let { builder ->
                            if (registrationValue.description.isNotBlank())
                                builder.setDescription(registrationValue.description)
                            else builder
                        }.let { builder ->
                            if (registrationValue.unit.isNotBlank())
                                builder.setUnit(registrationValue.unit)
                            else builder
                        }.let { builder ->
                            if (registrationValue !is MutableGaugeInstrumentRegistration<*>) {
                                throw IllegalArgumentException("Cannot register at OTel with non-mutable persistent instrument $registration during setup")
                            }
                            if (registrationValue.supportsFloating) {
                                builder.buildWithCallback(registrationValue::observe)
                            } else {
                                builder.ofLongs().buildWithCallback(registrationValue::observe)
                            }
                        }
                    runWithExceptionCleanup(otelRegistration::close) {
                        registrationValue.provideOTelRegistration(otelRegistration)
                        manager.triggerOwnInstrumentAdded(registrationValue, IInstrumentAvailabilityCallback.Phase.POST)
                    }
                }
            }
            return manager
        }
    }

    fun start() {
        allowRegistration.set(true)
    }

    fun stop() {
        allowRegistration.set(false)
        unregisterAllLocal()
    }

    override fun close() {
        stop()
    }

    override fun gaugeInstrument(name: String): WorldGaugeInstrumentBuilder {
        return WorldGaugeInstrumentBuilder(name, this)
    }

    override fun createImmutableDoubleRegistration(
        builder: WorldGaugeInstrumentBuilder,
        callback: IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>,
    ): ImmutableGaugeInstrumentRegistration {
        if (builder.persistent) throw IllegalArgumentException("Cannot create persistent immutable instrument registrations")
        assertAllowsRegistration()
        return WorldImmutableGaugeInstrumentRegistration(builder, supportsFloating = true, callback)
    }

    override fun createImmutableLongRegistration(
        builder: WorldGaugeInstrumentBuilder,
        callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>,
    ): ImmutableGaugeInstrumentRegistration {
        if (builder.persistent) throw IllegalArgumentException("Cannot create persistent immutable instrument registrations")
        assertAllowsRegistration()
        return WorldImmutableGaugeInstrumentRegistration(builder, supportsFloating = false, callback)
    }

    override fun createMutableDoubleRegistration(builder: WorldGaugeInstrumentBuilder): WorldMutableGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return WorldMutableGaugeInstrumentRegistration(builder, supportsFloating = true)
    }

    override fun createMutableLongRegistration(builder: WorldGaugeInstrumentBuilder): WorldMutableGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return WorldMutableGaugeInstrumentRegistration(builder, supportsFloating = false)
    }

    internal class WorldImmutableGaugeInstrumentRegistration: ImmutableGaugeInstrumentRegistration, IWorldInstrumentRegistration {
        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
            supportsFloating: Boolean,
            callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
        ) : super(name, description, unit, attributes, supportsFloating, callback)

        constructor(
            builder: GaugeInstrumentBuilder<*>,
            supportsFloating: Boolean,
            callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
        ) : super(builder, supportsFloating, callback)

        override val persistent: Boolean = false
    }

    internal class WorldGaugeInstrumentBuilder(
        name: String,
        manager: ServerWorldInstrumentManager,
    ) : GaugeInstrumentBuilder<WorldGaugeInstrumentBuilder>(
        name,
        @Suppress("UNCHECKED_CAST")
        (manager as InstrumentManagerBase<GaugeInstrumentBuilder<WorldGaugeInstrumentBuilder>>),
    ), IWorldGaugeInstrumentBuilder<WorldGaugeInstrumentBuilder> {

        override var persistent: Boolean = false
    }

    internal class WorldMutableGaugeInstrumentRegistration :
            MutableGaugeInstrumentRegistration<WorldMutableGaugeInstrumentRegistration>,
            IWorldMutableInstrumentRegistration<WorldMutableGaugeInstrumentRegistration> {

        override val persistent: Boolean

        constructor(builder: WorldGaugeInstrumentBuilder, supportsFloating: Boolean) : super(
            builder,
            supportsFloating,
        ) {
            this.persistent = builder.persistent
        }

        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
            supportsFloating: Boolean,
            persistent: Boolean,
        ) : super(name, description, unit, attributes, supportsFloating) {
            this.persistent = persistent
        }
    }

    class WorldInstrumentSavedData private constructor(
        backingMap: DirtyCallbackMutableMap.Concurrent<
                String,
                InstrumentManagerBaseRegistrationUnion
                >,
    ) : SavedDataConcurrentMap<String, InstrumentManagerBaseRegistrationUnion>(
        backingMap
    ) {

        companion object {

            operator fun invoke(data: Map<String, InstrumentManagerBaseRegistrationUnion> = emptyMap()): WorldInstrumentSavedData {
                var setDirty: () -> Unit = {}
                return WorldInstrumentSavedData(
                    DirtyCallbackMutableMap.Concurrent(
                        ConcurrentHashMap(
                            data
                        )
                    ) {
                        // do not collapse this one-call lambda into a reference!
                        // We are explicitly adding indirection here!
                        setDirty()
                    }
                ).also {
                    @Suppress("AssignedValueIsNeverRead") // value is read
                    setDirty = it::setDirty
                }
            }

            private fun createEntryTag(
                name: String,
                registration: InstrumentManagerBaseRegistrationUnion,
            ): CompoundTag? {
                val value = registration.value
                if (value !is IWorldMutableInstrumentRegistration<*>) return null
                if (!value.persistent) return null
                return CompoundTag().apply {
                    putString("name", name)
                    val integral: Boolean = !value.supportsFloating
                    putBoolean("integral", integral)
                    if (value.description.isNotEmpty()) {
                        putString("description", value.description)
                    }
                    if (value.unit.isNotEmpty()) {
                        putString("unit", value.unit)
                    }
                    if (value.attributes.isNotEmpty()) {
                        put("attributes", ListTag().apply {
                            for (attributeKey in value.attributes.values) {
                                this.add(attributeKey.save())
                            }
                        })
                    }
                }
            }

            private fun loadEntryTag(
                tag: CompoundTag,
                attributeKeyTypeHolderGetter: HolderGetter<IAttributeKeyTypeTemplate<*, *>>,
            ): Pair<String, InstrumentManagerBaseRegistrationUnion> {
                val name: String? = tag.getString("name")
                if (name.isNullOrEmpty()) throw NoSuchElementException("Could not find key 'name'")
                if (!tag.contains("integral", Tag.TAG_BYTE.toInt()))
                    throw NoSuchElementException("Could not find key 'integral'")
                val integral = tag.getBoolean("integral")
                val description: String = tag.getString("description").orEmpty()
                val unit: String = tag.getString("unit").orEmpty()
                val attributes: Map<String, MappedAttributeKeyInfo<*, *>> =
                    if (tag.contains("attributes", Tag.TAG_LIST.toInt())) {
                        buildMap {
                            tag.getList("attributes", Tag.TAG_COMPOUND.toInt()).forEach {
                                val keyInfo =
                                    MappedAttributeKeyInfo.Companion.load(it as CompoundTag, attributeKeyTypeHolderGetter)
                                this@buildMap.put(keyInfo.baseKey.key, keyInfo)
                            }
                        }
                    } else {
                        emptyMap()
                    }
                return name to Union2.Companion.of2(
                    WorldMutableGaugeInstrumentRegistration(
                        name = name,
                        description = description,
                        unit = unit,
                        attributes = attributes,
                        supportsFloating = !integral,
                        persistent = true,
                    )
                )
            }


            internal val Factory = Factory<WorldInstrumentSavedData>(
                ::invoke,
                ::load,
                DataFixTypes.LEVEL
            )

            fun load(tag: CompoundTag, lookupProvider: HolderLookup.Provider): WorldInstrumentSavedData {
                val attributeKeyTypeGetter: HolderGetter<IAttributeKeyTypeTemplate<*, *>> =
                    lookupProvider.lookupOrThrow(OTelCoreModAPI.AttributeTypeMappings)
                val data: Map<String, InstrumentManagerBaseRegistrationUnion> =
                    buildMap {
                        val dataTag = tag.getCompound("data") ?: return@buildMap
                        val listTag = dataTag.getList("instruments", ListTag.TAG_COMPOUND.toInt()) ?: return@buildMap
                        listTag.forEach {
                            val (name, loadedRegistration) = loadEntryTag(it as CompoundTag, attributeKeyTypeGetter)
                            this@buildMap.put(name, loadedRegistration)
                        }
                    }
                return WorldInstrumentSavedData(data)
            }
        }

        override fun save(
            compoundTag: CompoundTag,
            provider: HolderLookup.Provider,
            data: Map<String, InstrumentManagerBaseRegistrationUnion>,
        ): CompoundTag {
            compoundTag.put("data", CompoundTag().apply {
                this.put("instruments", ListTag().apply {
                    for ((name, registration) in data) {
                        this.add(createEntryTag(name, registration) ?: continue)
                    }
                })
            })
            return compoundTag
        }
    }
}
