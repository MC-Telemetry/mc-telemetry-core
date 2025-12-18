package de.mctelemetry.core.api.attributes

class ObservationContext<TL: IMappedAttributeValueLookup>(
    val attributeValueLookup: TL,
)
