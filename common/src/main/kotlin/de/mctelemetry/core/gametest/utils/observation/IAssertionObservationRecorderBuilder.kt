package de.mctelemetry.core.gametest.utils.observation

import de.mctelemetry.core.api.metrics.IObservationSource
import io.opentelemetry.api.common.Attributes
import org.jetbrains.annotations.Contract

interface IAssertionObservationRecorderBuilder {

    val supportsFloating: Boolean?
    var allowAdditional: Boolean // default false


    @Contract("_,_,_,_,_ -> this", mutates = "this")
    fun assertRecordsPreferred(
        attributes: Attributes,
        longValue: Long,
        doubleValue: Double = longValue.toDouble(),
        source: IObservationSource<*, *>? = null,
        requireSourceMatch: Boolean = source != null,
    ): IAssertionObservationRecorderBuilder

    interface ForDouble: IAssertionObservationRecorderBuilder {

        @Contract("_,_,_,_,_ -> this", mutates = "this")
        fun assertRecordsDouble(
            attributes: Attributes,
            doubleValue: Double,
            allowPreferred: Boolean = true,
            source: IObservationSource<*, *>? = null,
            requireSourceMatch: Boolean = source != null,
        ): IAssertionObservationRecorderBuilder
    }

    interface ForLong : IAssertionObservationRecorderBuilder {

        @Contract("_,_,_,_,_ -> this", mutates = "this")
        fun assertRecordsLong(
            attributes: Attributes,
            longValue: Long,
            allowPreferred: Boolean = true,
            source: IObservationSource<*, *>? = null,
            requireSourceMatch: Boolean = source != null,
        ): IAssertionObservationRecorderBuilder
    }
}
