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

    context(attributeStore: IMappedAttributeValueLookup)
    override fun observe(value: Double, source: IObservationSource<*, *>) {
        val attributeValues = attributeStore.references.mapNotNull {
            MappedAttributeKeyValue(
                it.info,
                try {
                    attributeStore[it] ?: return@mapNotNull null
                } catch (_: NoSuchElementException) {
                    return@mapNotNull null
                }
            )
        }
        val point = RecordedObservationPoint(MappedAttributeKeyMap(attributeValues), value)
        mapForSource(source)[attributeValues] = point
    }

    context(attributeStore: IMappedAttributeValueLookup)
    override fun observe(value: Long, source: IObservationSource<*, *>) {
        val attributeValues = attributeStore.references.mapNotNull {
            MappedAttributeKeyValue(
                it.info,
                try {
                    attributeStore[it] ?: return@mapNotNull null
                } catch (_: NoSuchElementException) {
                    return@mapNotNull null
                }
            )
        }
        val point = RecordedObservationPoint(MappedAttributeKeyMap(attributeValues), value)
        mapForSource(source)[attributeValues] = point
    }

    context(attributeStore: IMappedAttributeValueLookup)
    override fun observePreferred(
        double: Double,
        long: Long,
        source: IObservationSource<*, *>,
    ) {
        val attributeValues = attributeStore.references.mapNotNull {
            MappedAttributeKeyValue(
                it.info,
                try {
                    attributeStore[it] ?: return@mapNotNull null
                } catch (_: NoSuchElementException) {
                    return@mapNotNull null
                }
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
