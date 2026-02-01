package de.mctelemetry.core.observations

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class IORecorder<T> {

    private val usages: AtomicInteger = AtomicInteger(1)

    private fun incrementUsages(): Int = usages.incrementAndGet()
    private fun decrementUsages(): Int = usages.decrementAndGet()

    val inserted: AtomicLong = AtomicLong(0)
    val extracted: AtomicLong = AtomicLong(0)
    val pushed: AtomicLong = AtomicLong(0)
    val pulled: AtomicLong = AtomicLong(0)

    fun addExtracted(count: Int, type: T) = addExtracted(count.toLong(), type)
    fun addExtracted(count: Long, type: T) {
        extracted.addAndGet(count)
    }

    fun addInserted(count: Int, type: T) = addInserted(count.toLong(), type)
    fun addInserted(count: Long, type: T) {
        inserted.addAndGet(count)
    }
    fun addPushed(count: Int, type: T) = addPushed(count.toLong(), type)
    fun addPushed(count: Long, type: T) {
        pushed.addAndGet(count)
    }
    fun addPulled(count: Int, type: T) = addPulled(count.toLong(), type)
    fun addPulled(count: Long, type: T) {
        pulled.addAndGet(count)
    }

    class IORecorderAccess<T>(private val recorder: IORecorder<T>, initialIncrement: Boolean = true) :
        AutoCloseable {
        init {
            if (initialIncrement) {
                recorder.incrementUsages()
            }
        }

        fun getAbsoluteInserted(): Long = recorder.inserted.get()
        fun getAbsoluteExtracted(): Long = recorder.extracted.get()
        fun getAbsolutePushed(): Long = recorder.pushed.get()
        fun getAbsolutePulled(): Long = recorder.pulled.get()

        val insertedOffset: Long = getAbsoluteInserted()
        val extractedOffset: Long = getAbsoluteExtracted()
        val pushedOffset: Long = getAbsolutePushed()
        val pulledOffset: Long = getAbsolutePulled()

        fun getRelativeInserted(): Long = getAbsoluteInserted() - insertedOffset
        fun getRelativeExtracted(): Long = getAbsoluteExtracted() - extractedOffset
        fun getRelativePushed(): Long = getAbsolutePushed() - pushedOffset
        fun getRelativePulled(): Long = getAbsolutePulled() - pulledOffset

        var closed = false
            private set

        override fun close() {
            if (closed) return
            closed = true
            recorder.decrementUsages()
        }
    }
}
