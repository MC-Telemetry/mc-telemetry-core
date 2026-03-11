package de.mctelemetry.core.utils

object EmptyAutoCloseable : AutoCloseable {
    override fun close() {}
}
