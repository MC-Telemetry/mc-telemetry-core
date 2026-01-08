package de.mctelemetry.core.utils

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource

context(source: IObservationSource<*, *>, attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observe(value: Long) = observe(value, source)

context(source: IObservationSource<*, *>, attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observe(value: Double) = observe(value, source)

context(source: IObservationSource<*, *>, attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observe(value: Int) = observe(value.toLong(), source)

context(attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observe(value: Int, source: IObservationSource<*, *>) =
    observe(value.toLong(), source)

context(source: IObservationSource<*, *>, attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, long: Long) =
    observePreferred(double, long, source)

context(source: IObservationSource<*, *>, attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, int: Int) =
    observePreferred(double, int.toLong(), source)

context(attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, int: Int, source: IObservationSource<*, *>) =
    observePreferred(double, int.toLong(), source)


context(attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.Sourceless.observe(value: Int) = observe(value.toLong())

context(attributeStore: IMappedAttributeValueLookup)
fun IObservationRecorder.Unresolved.Sourceless.observePreferred(double: Double, int: Int) =
    observePreferred(double, int.toLong())
