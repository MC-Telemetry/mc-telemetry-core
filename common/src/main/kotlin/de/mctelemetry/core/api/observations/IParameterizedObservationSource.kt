package de.mctelemetry.core.api.observations

import com.mojang.brigadier.arguments.ArgumentType

interface IParameterizedObservationSource<SC, I : IObservationSourceInstance<SC, *, I>> : IObservationSource<SC, I> {

    val parameters: Map<String, ArgumentType<*>>

    fun instanceFromParameters(parameterMap: Map<String, *>): I
}
