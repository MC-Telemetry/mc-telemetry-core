package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.BoundDomainAttributeKeyInfo
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.persistence.DirtyCallbackMutableMap
import de.mctelemetry.core.persistence.SavedDataConcurrentMap
import de.mctelemetry.core.utils.Union3
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.AttributeType
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.opentelemetry.api.metrics.ObservableMeasurement
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.storage.DimensionDataStorage
import java.lang.AutoCloseable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class WorldInstrumentManager private constructor(
    meter: Meter,
    override val gameInstruments: IGameInstrumentManager,
    val server: MinecraftServer,
    localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion>,
) : InstrumentManagerBase<WorldInstrumentManager.WorldGaugeInstrumentBuilder>(
    meter,
    gameInstruments,
    localInstruments,
), IWorldInstrumentManager, AutoCloseable {

    constructor(meter: Meter, gameInstruments: IGameInstrumentManager, server: MinecraftServer) : this(
        meter,
        gameInstruments,
        server,
        ConcurrentHashMap()
    )

    companion object {

        private val WORLDMETRICS_KEY: String = "${OTelCoreMod.MOD_ID}_worldmetrics"

        fun withPersistentStorage(
            meter: Meter,
            gameInstruments: IGameInstrumentManager,
            server: MinecraftServer,
            dataStorage: DimensionDataStorage = server.getLevel(ServerLevel.OVERWORLD)!!.dataStorage,
        ): WorldInstrumentManager {
            val saveData = dataStorage.computeIfAbsent(WorldInstrumentSavedData.Factory, WORLDMETRICS_KEY)!!
            val manager = WorldInstrumentManager(meter, gameInstruments, server, saveData)
            runWithExceptionCleanup(manager::close) {
                for (registration in manager.localInstruments.values) {
                    val registrationValue = registration.value
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
                            when (registrationValue) {
                                is MutableLongGaugeInstrumentRegistration -> {
                                    builder.ofLongs().buildWithCallback(registrationValue::observe)
                                }
                                is MutableDoubleGaugeInstrumentRegistration -> {
                                    builder.buildWithCallback(registrationValue::observe)
                                }
                                else -> throw IllegalArgumentException("Cannot register at OTel with non-mutable persistent instrument $registration during setup")
                            }
                        }
                    runWithExceptionCleanup(otelRegistration::close) {
                        registrationValue.provideOTelRegistration(otelRegistration)
                    }
                }
            }
            return manager
        }
    }

    override val localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion>
        get() = super.localInstruments

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
        callback: IInstrumentRegistration.Callback<ObservableDoubleMeasurement>,
    ): ImmutableGaugeInstrumentRegistration<ObservableDoubleMeasurement> {
        if (builder.persistent) throw IllegalArgumentException("Cannot create persistent immutable instrument registrations")
        return super.createImmutableDoubleRegistration(builder, callback)
    }

    override fun createImmutableLongRegistration(
        builder: WorldGaugeInstrumentBuilder,
        callback: IInstrumentRegistration.Callback<ObservableLongMeasurement>,
    ): ImmutableGaugeInstrumentRegistration<ObservableLongMeasurement> {
        if (builder.persistent) throw IllegalArgumentException("Cannot create persistent immutable instrument registrations")
        return super.createImmutableLongRegistration(builder, callback)
    }

    override fun createMutableDoubleRegistration(builder: WorldGaugeInstrumentBuilder): WorldMutableDoubleGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return WorldMutableDoubleGaugeInstrumentRegistration(builder)
    }

    override fun createMutableLongRegistration(builder: WorldGaugeInstrumentBuilder): MutableLongGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return WorldMutableLongGaugeInstrumentRegistration(builder)
    }

    internal class WorldGaugeInstrumentBuilder(
        name: String,
        manager: WorldInstrumentManager,
    ) : GaugeInstrumentBuilder<WorldGaugeInstrumentBuilder>(
        name,
        @Suppress("UNCHECKED_CAST")
        (manager as InstrumentManagerBase<GaugeInstrumentBuilder<WorldGaugeInstrumentBuilder>>),
    ), IWorldGaugeInstrumentBuilder<WorldGaugeInstrumentBuilder> {

        override var persistent: Boolean = false
    }

    internal sealed interface IWorldMutableInstrumentRegistration<R : ObservableMeasurement> : IInstrumentRegistration.Mutable<R> {

        val persistent: Boolean
    }

    internal class WorldMutableLongGaugeInstrumentRegistration : MutableLongGaugeInstrumentRegistration,
            IWorldMutableInstrumentRegistration<ObservableLongMeasurement> {

        override val persistent: Boolean

        constructor(builder: WorldGaugeInstrumentBuilder) : super(
            builder
        ) {
            this.persistent = builder.persistent
        }

        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Map<String, BoundDomainAttributeKeyInfo<*>>,
            persistent: Boolean,
        ) : super(name, description, unit, attributes) {
            this.persistent = persistent
        }
    }

    internal class WorldMutableDoubleGaugeInstrumentRegistration : MutableDoubleGaugeInstrumentRegistration,
            IWorldMutableInstrumentRegistration<ObservableDoubleMeasurement> {

        override val persistent: Boolean

        constructor(builder: WorldGaugeInstrumentBuilder) : super(
            builder
        ) {
            this.persistent = builder.persistent
        }

        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Map<String, BoundDomainAttributeKeyInfo<*>>,
            persistent: Boolean,
        ) : super(name, description, unit, attributes) {
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
                    val integral: Boolean = when (value) {
                        is WorldMutableLongGaugeInstrumentRegistration -> true
                        is WorldMutableDoubleGaugeInstrumentRegistration -> false
                    }
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
                                this.add(createAttributeKeyInfoTag(attributeKey))
                            }
                        })
                    }
                }
            }

            private fun createAttributeKeyInfoTag(key: BoundDomainAttributeKeyInfo<*>): CompoundTag {
                return CompoundTag().apply {
                    putString("name", key.attributeKey.key)
                    if (key is BoundDomainAttributeKeyInfo.DomainKeyInfo<*>) {
                        TODO("Non-builtin keys cannot be serialized yet")
                    }
                    putString("type", key.attributeKey.type.name)
                }
            }

            private fun loadEntryTag(
                tag: CompoundTag,
            ): Pair<String, InstrumentManagerBaseRegistrationUnion> {
                val name: String? = tag.getString("name")
                if (name.isNullOrEmpty()) throw NoSuchElementException("Could not find key 'name'")
                if (!tag.contains("integral", Tag.TAG_BYTE.toInt()))
                    throw NoSuchElementException("Could not find key 'integral'")
                val integral = tag.getBoolean("integral")
                val description: String = tag.getString("description").orEmpty()
                val unit: String = tag.getString("unit").orEmpty()
                val attributes: Map<String, BoundDomainAttributeKeyInfo<*>> =
                    if (tag.contains("attributes", Tag.TAG_LIST.toInt())) {
                        buildMap {
                            tag.getList("attributes", Tag.TAG_COMPOUND.toInt()).forEach {
                                val keyInfo = loadAttributeKeyInfoTag(it as CompoundTag)
                                this@buildMap.put(keyInfo.attributeKey.key, keyInfo)
                            }
                        }
                    } else {
                        emptyMap()
                    }
                return name to if (integral)
                    Union3.of2(
                        WorldMutableLongGaugeInstrumentRegistration(
                            name = name,
                            description = description,
                            unit = unit,
                            attributes = attributes,
                            persistent = true,
                        )
                    )
                else
                    Union3.of3(
                        WorldMutableDoubleGaugeInstrumentRegistration(
                            name = name,
                            description = description,
                            unit = unit,
                            attributes = attributes,
                            persistent = true,
                        )
                    )
            }


            private fun loadAttributeKeyInfoTag(tag: CompoundTag): BoundDomainAttributeKeyInfo<*> {
                val name = tag.getString("name")
                if (name.isNullOrEmpty()) throw NoSuchElementException("Could not find key 'name'")
                val type = tag.getString("type")
                if (type.isNullOrEmpty()) throw NoSuchElementException("Could not find key 'type'")
                val enumType = AttributeType.valueOf(type)
                return BoundDomainAttributeKeyInfo.ofBuiltin(enumType, name)
            }

            internal val Factory = Factory<WorldInstrumentSavedData>(
                ::invoke,
                ::load,
                DataFixTypes.LEVEL
            )

            fun load(tag: CompoundTag, lookupProvider: HolderLookup.Provider): WorldInstrumentSavedData {
                val data: Map<String, InstrumentManagerBaseRegistrationUnion> =
                    buildMap {
                        val dataTag = tag.getCompound("data") ?: return@buildMap
                        val listTag = dataTag.getList("instruments", ListTag.TAG_COMPOUND.toInt()) ?: return@buildMap
                        listTag.forEach {
                            val (name, loadedRegistration) = loadEntryTag(it as CompoundTag)
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
