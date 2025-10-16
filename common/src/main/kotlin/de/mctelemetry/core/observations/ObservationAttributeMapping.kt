package de.mctelemetry.core.observations

import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.metrics.IMappedAttributeKeyType
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.canConvertTo
import net.minecraft.network.chat.MutableComponent

class ObservationAttributeMapping(
    /**
     * This map is structured in "callback-direction" from the metric-attributes to the observation-attributes of the source.
     * Given a metric with attributes `A` and `B` and an observation source with attributes `X`, `Y` and `Z`, a valid
     * mapping might look like this:
     * ```json
     * {
     *   "A": "X",
     *   "B": "Z"
     * }
     *  ```
     *  where the observation attribute `Y` was not mapped to any metric attribute. While observation attributes are
     *  optional, all metric attributes MUST be assigned a type-assignable (see below for conversion) observation
     *  attribute.
     *
     *  The types of observation and metric attributes don't need to match exactly, so long as the observation attribute
     *  type is assignable to the type of the metric attribute. Type `X` is considered assignable to type `B` IFF at
     *  least one of the following is true:
     *  - `X === A`, which will result in no type-conversion
     *  - `X.assignableTo(A)`, which will result in the conversion `it: X -> X.convertTo(A, it) as A`
     *  - `A.assignableFrom(X)`, which will result in the conversion `it: X -> A.convertFrom(X, it) as A`
     **/
    val mapping: Map<MappedAttributeKeyInfo<*, *>, MappedAttributeKeyInfo<*, *>>,
) {

    fun validateAssignments(): MutableComponent? {
        for ((target, source) in mapping) {
            if (!(source.type canConvertTo target.type))
                return TranslationKeys.Errors.attributeTypesIncompatible(source, target)
        }
        return null
    }

    fun validateStatic(): MutableComponent? {
        return validateAssignments()
    }

    fun validateDynamic(targetAttributes: Set<MappedAttributeKeyInfo<*, *>>): MutableComponent? {
        for (target in targetAttributes) {
            if (target !in mapping) {
                return TranslationKeys.Errors.attributeMappingMissing(target)
            }
        }
        return null
    }


    fun validate(targetAttributes: Set<MappedAttributeKeyInfo<*, *>>): MutableComponent? {
        return validateStatic() ?: validateDynamic(targetAttributes)
    }
}
