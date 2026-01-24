package de.mctelemetry.core.api.attributes

import io.opentelemetry.api.common.AttributeType

sealed class GenericAttributeType<T : Any>(
    internal val otelType: AttributeType,
    internal val mappedType: IAttributeKeyTypeTemplate<T, T, *>,
) {

    object STRING : GenericAttributeType<String>(
        AttributeType.STRING,
        NativeAttributeKeyTypes.StringType
    )

    object BOOLEAN : GenericAttributeType<Boolean>(
        AttributeType.BOOLEAN,
        NativeAttributeKeyTypes.BooleanType
    )

    object LONG : GenericAttributeType<Long>(
        AttributeType.LONG,
        NativeAttributeKeyTypes.LongType
    )

    object DOUBLE : GenericAttributeType<Double>(
        AttributeType.DOUBLE,
        NativeAttributeKeyTypes.DoubleType
    )

    object STRING_ARRAY : GenericAttributeType<List<String>>(
        AttributeType.STRING_ARRAY,
        NativeAttributeKeyTypes.StringArrayType
    )

    object BOOLEAN_ARRAY : GenericAttributeType<List<Boolean>>(
        AttributeType.BOOLEAN_ARRAY,
        NativeAttributeKeyTypes.BooleanArrayType
    )

    object LONG_ARRAY : GenericAttributeType<List<Long>>(
        AttributeType.LONG_ARRAY,
        NativeAttributeKeyTypes.LongArrayType
    )

    object DOUBLE_ARRAY : GenericAttributeType<List<Double>>(
        AttributeType.DOUBLE_ARRAY,
        NativeAttributeKeyTypes.DoubleArrayType
    )
}
