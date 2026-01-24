package de.mctelemetry.core.persistence

import de.mctelemetry.core.api.observations.IObservationSource
import io.wispforest.endec.SerializationAttribute

object SerializationAttributes {
    val ObservationSourceSerializationAttribute =
        SerializationAttribute.withValue<IObservationSource<*, *>>("mcotelcore:observation_source")
}
