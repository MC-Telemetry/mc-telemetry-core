package de.mctelemetry.core.utils

infix fun <T> MutableList<T>?.nullablePlus(element: T): MutableList<T> {
    return if (this == null) {
        mutableListOf(element)
    } else {
        add(element)
        this
    }
}
