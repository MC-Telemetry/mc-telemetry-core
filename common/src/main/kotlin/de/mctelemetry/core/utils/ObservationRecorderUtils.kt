package de.mctelemetry.core.utils

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IObservationSourceInstance

context(source: IObservationSourceInstance<*, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Long) = observe(value, source)

context(source: IObservationSourceInstance<*, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Double) = observe(value, source)

context(source: IObservationSourceInstance<*, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Int) = observe(value.toLong(), source)

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Int, source: IObservationSourceInstance<*, *>) =
    observe(value.toLong(), source)

context(source: IObservationSourceInstance<*, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, long: Long) =
    observePreferred(double, long, source)

context(source: IObservationSourceInstance<*, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, int: Int) =
    observePreferred(double, int.toLong(), source)

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, int: Int, source: IObservationSourceInstance<*, *>) =
    observePreferred(double, int.toLong(), source)


context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.Sourceless.observe(value: Int) = observe(value.toLong())

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.Sourceless.observePreferred(double: Double, int: Int) =
    observePreferred(double, int.toLong())
