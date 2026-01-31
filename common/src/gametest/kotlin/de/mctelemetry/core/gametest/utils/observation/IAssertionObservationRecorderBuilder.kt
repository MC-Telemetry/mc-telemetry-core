package de.mctelemetry.core.gametest.utils.observation

import de.mctelemetry.core.api.observations.IObservationSourceInstance
import io.opentelemetry.api.common.Attributes
import org.jetbrains.annotations.Contract

interface IAssertionObservationRecorderBuilder {

    val supportsFloating: Boolean?
    var allowAdditional: Boolean // default false


    @Contract("_,_,_,_,_ -> this")
    fun assertRecordsPreferred(
        attributes: Attributes,
        longValue: Long,
        doubleValue: Double = longValue.toDouble(),
        sourceInstance: IObservationSourceInstance<*, *, *>? = null,
        requireSourceInstanceMatch: Boolean = sourceInstance != null,
    ): IAssertionObservationRecorderBuilder

    interface ForDouble: IAssertionObservationRecorderBuilder {

        @Contract("_,_,_,_,_ -> this")
        fun assertRecordsDouble(
            attributes: Attributes,
            doubleValue: Double,
            allowPreferred: Boolean = true,
            sourceInstance: IObservationSourceInstance<*, *, *>? = null,
            requireSourceInstanceMatch: Boolean = sourceInstance != null,
        ): IAssertionObservationRecorderBuilder
    }

    interface ForLong : IAssertionObservationRecorderBuilder {

        @Contract("_,_,_,_,_ -> this")
        fun assertRecordsLong(
            attributes: Attributes,
            longValue: Long,
            allowPreferred: Boolean = true,
            sourceInstance: IObservationSourceInstance<*, *, *>? = null,
            requireSourceInstanceMatch: Boolean = sourceInstance != null,
        ): IAssertionObservationRecorderBuilder
    }
}
