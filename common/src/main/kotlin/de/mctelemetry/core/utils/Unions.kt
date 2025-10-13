package de.mctelemetry.core.utils

sealed interface Union2<T1 : C, T2 : C, out C> {
    data class UnionT1<T1 : C, T2 : C, C>(override val value: T1) : Union2<T1, T2, C>
    data class UnionT2<T1 : C, T2 : C, C>(override val value: T2) : Union2<T1, T2, C>

    companion object {

        fun <T1 : C, T2 : C, T3 : C, C> of1(value: T1) = UnionT1<T1, T2, C>(value)
        fun <T1 : C, T2 : C, T3 : C, C> of2(value: T2) = UnionT2<T1, T2, C>(value)
    }

    val value: C
}

sealed interface Union3<T1 : C, T2 : C, T3 : C, out C> {
    data class UnionT1<T1 : C, T2 : C, T3 : C, C>(override val value: T1) : Union3<T1, T2, T3, C>
    data class UnionT2<T1 : C, T2 : C, T3 : C, C>(override val value: T2) : Union3<T1, T2, T3, C>
    data class UnionT3<T1 : C, T2 : C, T3 : C, C>(override val value: T3) : Union3<T1, T2, T3, C>

    companion object {

        fun <T1 : C, T2 : C, T3 : C, C> of1(value: T1) = UnionT1<T1, T2, T3, C>(value)
        fun <T1 : C, T2 : C, T3 : C, C> of2(value: T2) = UnionT2<T1, T2, T3, C>(value)
        fun <T1 : C, T2 : C, T3 : C, C> of3(value: T3) = UnionT3<T1, T2, T3, C>(value)
    }

    val value: C
}
