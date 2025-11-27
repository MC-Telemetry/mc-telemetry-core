package de.mctelemetry.core.network.observations.container.observationrequest

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class MemoryObservationRecorder : IObservationRecorder.Unresolved {

    private val backingMap: ConcurrentMap<IObservationSource<*, *>, ConcurrentMap<List<MappedAttributeKeyValue<*, *>>, RecordedObservationPoint>> =
        ConcurrentHashMap()

    private fun mapForSource(source: IObservationSource<*, *>): ConcurrentMap<List<MappedAttributeKeyValue<*, *>>, RecordedObservationPoint> {
        return backingMap.computeIfAbsent(source) { ConcurrentHashMap() }
    }

    fun recordedAsMap(): Map<IObservationSource<*, *>, RecordedObservations> {
        return backingMap.mapValues { (_, subMaps) ->
            RecordedObservations(null, subMaps)
        }
    }

    override fun onNewSource(source: IObservationSource<*, *>) {
        mapForSource(source)
    }

    fun clear() {
        backingMap.clear()
    }

    override fun observe(value: Double, attributes: IMappedAttributeValueLookup, source: IObservationSource<*, *>) {
        val attributeValues = attributes.attributeKeys.map {
            MappedAttributeKeyValue(
                it,
                attributes[it] ?: throw NoSuchElementException("Could not find $it in $attributes")
            )
        }
        val point = RecordedObservationPoint(MappedAttributeKeyMap(attributeValues), value)
        mapForSource(source)[attributeValues] = point
    }

    override fun observe(value: Long, attributes: IMappedAttributeValueLookup, source: IObservationSource<*, *>) {
        val attributeValues = attributes.attributeKeys.map {
            MappedAttributeKeyValue(
                it,
                attributes[it] ?: throw NoSuchElementException("Could not find $it in $attributes")
            )
        }
        val point = RecordedObservationPoint(MappedAttributeKeyMap(attributeValues), value)
        mapForSource(source)[attributeValues] = point
    }

    override fun observePreferred(
        double: Double,
        long: Long,
        attributes: IMappedAttributeValueLookup,
        source: IObservationSource<*, *>,
    ) {
        val attributeValues = attributes.attributeKeys.map {
            MappedAttributeKeyValue(
                it,
                attributes[it] ?: throw NoSuchElementException("Could not find $it in $attributes")
            )
        }
        val point = RecordedObservationPoint(
            MappedAttributeKeyMap(attributeValues),
            doubleValue = double,
            longValue = long,
        )
        mapForSource(source)[attributeValues] = point
    }

    override val supportsFloating: Boolean
        get() = true
}
