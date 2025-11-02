package de.mctelemetry.core.utils.gametest

import de.mctelemetry.core.api.metrics.IObservationRecorder
import de.mctelemetry.core.api.metrics.IObservationSource
import io.opentelemetry.api.common.Attributes
import net.minecraft.gametest.framework.GameTestHelper

abstract class AssertionObservationRecorder(
    protected val gameTestHelper: GameTestHelper,
) : IObservationRecorder.Resolved {


    abstract override fun observePreferred(
        double: Double,
        long: Long,
        attributes: Attributes,
        source: IObservationSource<*, *>?,
    )

    abstract override fun observe(value: Long, attributes: Attributes, source: IObservationSource<*, *>?)

    abstract override fun observe(value: Double, attributes: Attributes, source: IObservationSource<*, *>?)

    class Single(
        gameTestHelper: GameTestHelper,
        val name: String,
        val longValue: Long?,
        val doubleValue: Double?,
        val attributes: Attributes,
        val allowRecordLong: Boolean = true,
        val allowRecordDouble: Boolean = true,
        val allowRecordPreferred: Boolean = true,
        val source: IObservationSource<*, *>? = null,
        val requireSourceMatch: Boolean = source != null,
        override val supportsFloating: Boolean = doubleValue != null,
    ) : AssertionObservationRecorder(gameTestHelper) {

        var sawValue: Boolean = false
            private set

        init {
            require(allowRecordLong || allowRecordDouble || allowRecordPreferred) {
                "At least one of allowRecordLong, allowRecordDouble and allowRecordPreferred must be true"
            }
            if (allowRecordLong) require(longValue != null) {
                "longValue must be non-null if allowRecordLong is true"
            }
            if (allowRecordDouble) require(doubleValue != null) {
                "doubleValue must be non-null if allowRecordDouble is true"
            }
            if (allowRecordPreferred) require(longValue != null || doubleValue != null) {
                "At least one of longValue and doubleValue must be non-null if allowRecordPreferred is true"
            }
            if (supportsFloating) require(doubleValue != null) {
                "doubleValue must be non-null if supportsFloating is true"
            }
            if (supportsFloating) require(allowRecordDouble || allowRecordPreferred) {
                "At least one of allowRecordDouble and allowRecordPreferred must be true if supportsFloating is true"
            }
        }

        fun assertSawValue() {
            gameTestHelper.assertTrueC(sawValue, "Expected observation but received none")
        }

        private fun nameWithSource(source: IObservationSource<*, *>?): String {
            if (requireSourceMatch) {
                if (this.source == null) {
                    if (source != null) {
                        gameTestHelper.failC("Expected source of $name to be null but was $source")
                    }
                } else {
                    gameTestHelper.assertValueEqualC(this.source, source, "source of $name")
                }
            }
            return if (source == null)
                name
            else
                "$name (from $source)"
        }

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val sourceName = nameWithSource(source)
            gameTestHelper.assertFalseC(sawValue, "Unexpected observation from $sourceName after only one was expected")
            if (!allowRecordPreferred) gameTestHelper.failC("Unexpected preferred observation for $sourceName")
            if (this.doubleValue != null) {
                gameTestHelper.assertValueEqualC(this.doubleValue, double, sourceName)
            }
            if (this.longValue != null) {
                gameTestHelper.assertValueEqualC(this.longValue, long, sourceName)
            }
            sawValue = true
        }

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val sourceName = nameWithSource(source)
            gameTestHelper.assertFalseC(sawValue, "Unexpected observation from $sourceName after only one was expected")
            if (!allowRecordLong) gameTestHelper.failC("Unexpected long observation for $sourceName")
            if (this.longValue != null) {
                gameTestHelper.assertValueEqualC(this.longValue, value, sourceName)
            }
            sawValue = true
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val sourceName = nameWithSource(source)
            if (!allowRecordDouble) gameTestHelper.failC("Unexpected double observation for $sourceName")
            if (this.doubleValue != null) {
                gameTestHelper.assertValueEqualC(this.doubleValue, value, sourceName)
            }
            gameTestHelper.assertFalseC(sawValue, "Unexpected observation from $sourceName after only one was expected")
            sawValue = true
        }

        companion object {

            fun assertRecordsDouble(
                gameTestHelper: GameTestHelper,
                name: String,
                doubleValue: Double,
                attributes: Attributes = Attributes.empty(),
                allowPreferred: Boolean = true,
                source: IObservationSource<*, *>? = null,
                requireSourceMatch: Boolean = source != null,
            ) = Single(
                gameTestHelper = gameTestHelper,
                name = name,
                longValue = null,
                doubleValue = doubleValue,
                attributes = attributes,
                allowRecordLong = false,
                allowRecordDouble = true,
                allowRecordPreferred = allowPreferred,
                source = source,
                requireSourceMatch = requireSourceMatch
            )

            fun assertRecordsLong(
                gameTestHelper: GameTestHelper,
                name: String,
                longValue: Long,
                attributes: Attributes = Attributes.empty(),
                allowPreferred: Boolean = true,
                source: IObservationSource<*, *>? = null,
                requireSourceMatch: Boolean = source != null,
            ) = Single(
                gameTestHelper = gameTestHelper,
                name = name,
                longValue = longValue,
                doubleValue = null,
                attributes = attributes,
                allowRecordLong = true,
                allowRecordDouble = false,
                allowRecordPreferred = allowPreferred,
                source = source,
                requireSourceMatch = requireSourceMatch
            )

            fun assertRecordsPreferred(
                gameTestHelper: GameTestHelper,
                name: String,
                longValue: Long,
                doubleValue: Double = longValue.toDouble(),
                attributes: Attributes = Attributes.empty(),
                source: IObservationSource<*, *>? = null,
                requireSourceMatch: Boolean = source != null,
            ) = Single(
                gameTestHelper = gameTestHelper,
                name = name,
                longValue = longValue,
                doubleValue = doubleValue,
                attributes = attributes,
                allowRecordLong = false,
                allowRecordDouble = false,
                allowRecordPreferred = true,
                source = source,
                requireSourceMatch = requireSourceMatch
            )
        }
    }
}
