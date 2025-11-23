package de.mctelemetry.core.network.observations.container.observationrequest

import de.mctelemetry.core.api.metrics.MappedAttributeKeyMap

class RecordedObservationPoint {

    val attributes: MappedAttributeKeyMap<*>
    val hasDouble: Boolean
    private val _doubleValue: Double
    val doubleValue: Double
        get() = if (hasDouble) _doubleValue else throw IllegalStateException("No double value recorded for $this")
    val doubleValueOrNull: Double?
        get() = if (hasDouble) _doubleValue else null
    val hasLong: Boolean
    private val _longValue: Long
    val longValue: Long
        get() = if (hasLong) _longValue else throw IllegalStateException("No long value recorded for $this")
    val longValueOrNull: Long?
        get() = if (hasLong) _longValue else null

    constructor(attributes: MappedAttributeKeyMap<*>, doubleValue: Double) {
        this.attributes = attributes
        this.hasDouble = true
        this._doubleValue = doubleValue
        this.hasLong = false
        this._longValue = 0L
    }

    constructor(attributes: MappedAttributeKeyMap<*>, longValue: Long) {
        this.attributes = attributes
        this.hasDouble = false
        this._doubleValue = 0.0
        this.hasLong = true
        this._longValue = longValue
    }

    constructor(attributes: MappedAttributeKeyMap<*>, doubleValue: Double, longValue: Long) {
        this.attributes = attributes
        this.hasDouble = true
        this._doubleValue = doubleValue
        this.hasLong = true
        this._longValue = longValue
    }

    constructor(attributes: MappedAttributeKeyMap<*>, doubleValue: Double?, longValue: Long?) {
        this.attributes = attributes
        this.hasDouble = doubleValue != null
        this._doubleValue = if (hasDouble) doubleValue else 0.0
        this.hasLong = longValue != null
        this._longValue = if (hasLong) longValue else 0L
        require(doubleValue != null || longValue != null) { "No values provided for $this" }
    }
}
