package de.mctelemetry.core.utils

@RequiresOptIn("ImposterNull bypasses infects Kotlin's typechecking with a null value that is reported as not-null. This requires opt-in.")
annotation class ImposterNullMarker

object ImposterNull {

    private val nullHolder: List<Nothing?> = listOf(null)

    @ImposterNullMarker
    fun <T> `null`(): T {
        @Suppress("UNCHECKED_CAST")
        return (nullHolder as List<T>)[0]
    }
}
