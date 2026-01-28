package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservationPoint
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservations
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class MemoryObservationRecorder(val mapping: ObservationAttributeMapping) : IObservationRecorder.Unresolved {

    private val backingMap: ConcurrentMap<IObservationSourceInstance<*, *, *>, ConcurrentMap<List<MappedAttributeKeyValue<*, *>>, RecordedObservationPoint>> =
        ConcurrentHashMap()

    private fun mapForSource(source: IObservationSourceInstance<*, *, *>): ConcurrentMap<List<MappedAttributeKeyValue<*, *>>, RecordedObservationPoint> {
        return backingMap.computeIfAbsent(source) { ConcurrentHashMap() }
    }

    fun recordedAsMap(): Map<IObservationSourceInstance<*, *, *>, RecordedObservations> {
        return backingMap.mapValues { (_, subMaps) ->
            RecordedObservations(null, subMaps)
        }
    }

    override fun onNewSource(source: IObservationSourceInstance<*, *, *>) {
        mapForSource(source)
    }

    fun clear() {
        backingMap.clear()
    }

    context(attributeStore: IAttributeValueStore)
    override fun observe(value: Double, sourceInstance: IObservationSourceInstance<*, *, *>) {
        val attributeValues = mapping.resolveAttributesToKeyValues()
        val point = RecordedObservationPoint(MappedAttributeKeyMap(attributeValues), value)
        mapForSource(sourceInstance)[attributeValues] = point
    }

    context(attributeStore: IAttributeValueStore)
    override fun observe(value: Long, sourceInstance: IObservationSourceInstance<*, *, *>) {
        val attributeValues = mapping.resolveAttributesToKeyValues()
        val point = RecordedObservationPoint(MappedAttributeKeyMap(attributeValues), value)
        mapForSource(sourceInstance)[attributeValues] = point
    }

    context(attributeStore: IAttributeValueStore)
    override fun observePreferred(
        double: Double,
        long: Long,
        sourceInstance: IObservationSourceInstance<*, *, *>,
    ) {
        val attributeValues = mapping.resolveAttributesToKeyValues()
        val point = RecordedObservationPoint(
            MappedAttributeKeyMap(attributeValues),
            doubleValue = double,
            longValue = long,
        )
        mapForSource(sourceInstance)[attributeValues] = point
    }

    override val supportsFloating: Boolean
        get() = true
}
