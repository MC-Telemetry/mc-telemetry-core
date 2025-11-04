package de.mctelemetry.core.utils.gametest.observation

import de.mctelemetry.core.api.metrics.IObservationRecorder
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.utils.gametest.assertFalseC
import de.mctelemetry.core.utils.gametest.assertTrueC
import de.mctelemetry.core.utils.gametest.assertValueEqualC
import de.mctelemetry.core.utils.gametest.failC
import io.opentelemetry.api.common.Attributes
import net.minecraft.gametest.framework.GameTestHelper
import java.util.concurrent.atomic.AtomicBoolean

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

    abstract fun assertSawAll()

    companion object {
        @PublishedApi
        internal class BuilderImpl(
            private val gameTestHelper: GameTestHelper,
            val name: String,
            override val supportsFloating: Boolean,
            override var allowAdditional: Boolean = false,
        ) : IAssertionObservationRecorderBuilder.ForLong, IAssertionObservationRecorderBuilder.ForDouble {

            private val subObservers: MutableList<Single> = mutableListOf()
            private var cannotSupportLongReason: Single? = null
            private var cannotSupportDoubleReason: Single? = null
            private var cannotSupportPreferredReason: Single? = null

            fun build(): AssertionObservationRecorder {
                if (allowAdditional) {
                    return Multi(
                        gameTestHelper,
                        name,
                        subObservers,
                        allowAdditional = true,
                        supportsFloating = supportsFloating,
                    )
                }
                if (subObservers.isEmpty()) {
                    return None(gameTestHelper, name, supportsFloating)
                }
                return subObservers.singleOrNull() ?: Multi(
                    gameTestHelper,
                    name,
                    subObservers,
                    allowAdditional = false,
                    supportsFloating = supportsFloating,
                )
            }

            override fun assertRecordsDouble(
                attributes: Attributes,
                doubleValue: Double,
                allowPreferred: Boolean,
                source: IObservationSource<*, *>?,
                requireSourceMatch: Boolean,
            ): IAssertionObservationRecorderBuilder {
                require(cannotSupportDoubleReason == null || allowPreferred) {
                    "Cannot add assertRecordsDouble without allowPreferred because doubles cannot be supported by all assertions: $cannotSupportDoubleReason"
                }
                subObservers.add(
                    Single.assertRecordsDouble(
                        gameTestHelper,
                        name,
                        doubleValue,
                        attributes,
                        allowPreferred = allowPreferred,
                        source = source,
                        requireSourceMatch = requireSourceMatch,
                    ).also {
                        if (!allowPreferred) {
                            cannotSupportLongReason = cannotSupportLongReason ?: it
                            cannotSupportPreferredReason = cannotSupportPreferredReason ?: it
                        }
                    }
                )
                return this
            }

            override fun assertRecordsLong(
                attributes: Attributes,
                longValue: Long,
                allowPreferred: Boolean,
                source: IObservationSource<*, *>?,
                requireSourceMatch: Boolean,
            ): IAssertionObservationRecorderBuilder {
                require(cannotSupportLongReason == null || allowPreferred) {
                    "Cannot add assertRecordsDouble without allowPreferred because longs cannot be supported by all assertions: $cannotSupportLongReason"
                }
                subObservers.add(
                    Single.assertRecordsLong(
                        gameTestHelper,
                        name,
                        longValue,
                        attributes,
                        allowPreferred = allowPreferred,
                        source = source,
                        requireSourceMatch = requireSourceMatch,
                    ).also {
                        if (!allowPreferred) {
                            cannotSupportDoubleReason = cannotSupportDoubleReason ?: it
                            cannotSupportPreferredReason = cannotSupportPreferredReason ?: it
                        }
                    }
                )
                return this
            }

            override fun assertRecordsPreferred(
                attributes: Attributes,
                longValue: Long,
                doubleValue: Double,
                source: IObservationSource<*, *>?,
                requireSourceMatch: Boolean,
            ): IAssertionObservationRecorderBuilder {
                require(cannotSupportPreferredReason == null) {
                    "Cannot add assertRecordsPreferred because preferred is not supported by all assertions: $cannotSupportPreferredReason"
                }
                subObservers.add(
                    Single.assertRecordsPreferred(
                        gameTestHelper,
                        name,
                        longValue,
                        doubleValue,
                        attributes,
                        source = source,
                        requireSourceMatch = requireSourceMatch,
                    ).also {
                        if (cannotSupportLongReason != null)
                            cannotSupportLongReason = it
                        if (cannotSupportDoubleReason != null)
                            cannotSupportDoubleReason = it
                    }
                )
                return this
            }
        }

        inline fun buildAssertionRecorder(
            gameTestHelper: GameTestHelper,
            name: String,
            supportsFloating: Boolean,
            block: IAssertionObservationRecorderBuilder.() -> Unit,
        ): AssertionObservationRecorder {
            return BuilderImpl(gameTestHelper, name, supportsFloating).apply(block).build()
        }

        inline fun buildAssertionRecorderDouble(
            gameTestHelper: GameTestHelper,
            name: String,
            block: IAssertionObservationRecorderBuilder.ForDouble.() -> Unit,
        ): AssertionObservationRecorder {
            return BuilderImpl(gameTestHelper, name, supportsFloating = true).apply(block).build()
        }

        inline fun buildAssertionRecorderLong(
            gameTestHelper: GameTestHelper,
            name: String,
            block: IAssertionObservationRecorderBuilder.ForLong.() -> Unit,
        ): AssertionObservationRecorder {
            return BuilderImpl(gameTestHelper, name, supportsFloating = false).apply(block).build()
        }
    }

    class None(
        gameTestHelper: GameTestHelper,
        val name: String,
        override val supportsFloating: Boolean,
    ) : AssertionObservationRecorder(gameTestHelper) {

        override fun assertSawAll() {}

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            if (source == null) {
                gameTestHelper.failC("Expected no observations for $name, but received preferred of $double and $long at $attributes")
            } else {
                gameTestHelper.failC("Expected no observations for $name, but received preferred of $double and $long at $attributes from $source")
            }
        }

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            if (source == null) {
                gameTestHelper.failC("Expected no observations for $name, but received long $value at $attributes")
            } else {
                gameTestHelper.failC("Expected no observations for $name, but received long of $value at $attributes from $source")
            }
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            if (source == null) {
                gameTestHelper.failC("Expected no observations for $name, but received double $value at $attributes")
            } else {
                gameTestHelper.failC("Expected no observations for $name, but received double of $value at $attributes from $source")
            }
        }
    }

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

        var sawValue: Boolean
            private set(value) {
                sawValueField.set(value)
            }
            get() = sawValueField.get()

        private val sawValueField = AtomicBoolean(false)

        override fun assertSawAll() {
            gameTestHelper.assertTrueC(sawValue, "Expected observation for $attributes on $name but received none")
        }

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

        private fun nameWithSource(source: IObservationSource<*, *>?): String {
            if (requireSourceMatch) {
                gameTestHelper.assertValueEqualC(source, this.source, "source of $name")
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
            gameTestHelper.assertTrueC(allowRecordPreferred, "Unexpected preferred observation for $sourceName")
            gameTestHelper.assertValueEqualC(attributes, this.attributes, "attributes for $sourceName")
            if (this.doubleValue != null) {
                gameTestHelper.assertValueEqualC(double, this.doubleValue, sourceName)
            }
            if (this.longValue != null) {
                gameTestHelper.assertValueEqualC(long, this.longValue, sourceName)
            }
            gameTestHelper.assertTrueC(
                sawValueField.compareAndSet(false, true),
                "Unexpected observation from $sourceName after only one was expected"
            )
        }

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val sourceName = nameWithSource(source)
            gameTestHelper.assertFalseC(sawValue, "Unexpected observation from $sourceName after only one was expected")
            gameTestHelper.assertTrueC(allowRecordLong, "Unexpected long observation for $sourceName")
            gameTestHelper.assertValueEqualC(attributes, this.attributes, "attributes for $sourceName")
            if (this.longValue != null) {
                gameTestHelper.assertValueEqualC(value, this.longValue, sourceName)
            }
            gameTestHelper.assertTrueC(
                sawValueField.compareAndSet(false, true),
                "Unexpected observation from $sourceName after only one was expected"
            )
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val sourceName = nameWithSource(source)
            gameTestHelper.assertFalseC(sawValue, "Unexpected observation from $sourceName after only one was expected")
            gameTestHelper.assertTrueC(allowRecordDouble, "Unexpected double observation for $sourceName")
            gameTestHelper.assertValueEqualC(attributes, this.attributes, "attributes for $sourceName")
            if (this.doubleValue != null) {
                gameTestHelper.assertValueEqualC(value, this.doubleValue, sourceName)
            }
            gameTestHelper.assertFalseC(sawValue, "Unexpected observation from $sourceName after only one was expected")
            gameTestHelper.assertTrueC(
                sawValueField.compareAndSet(false, true),
                "Unexpected observation from $sourceName after only one was expected"
            )
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

    class Multi private constructor(
        gameTestHelper: GameTestHelper,
        val name: String,
        private val subObservers: Map<Attributes, Single>,
        val allowAdditional: Boolean = false,
        override val supportsFloating: Boolean = subObservers.values.any(Single::supportsFloating),
    ) : AssertionObservationRecorder(gameTestHelper) {

        constructor(
            gameTestHelper: GameTestHelper,
            name: String,
            subObservers: Collection<Single>,
            allowAdditional: Boolean = false,
            supportsFloating: Boolean = subObservers.any(Single::supportsFloating),
        ) : this(
            gameTestHelper,
            name,
            subObservers.associateBy { it.attributes },
            allowAdditional = allowAdditional,
            supportsFloating = supportsFloating,
        ) {
            for (observer in subObservers) {
                val stored = this.subObservers[observer.attributes]
                require(stored === observer) { "All observers must have unique attributes. Both $stored and $observer have ${observer.attributes}" }
            }
        }

        init {
            for ((attr, observer) in subObservers) {
                assert(attr == observer.attributes)
            }
        }

        override fun assertSawAll() {
            subObservers.values.forEach(AssertionObservationRecorder::assertSawAll)
        }

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val observer = subObservers.getOrElse(attributes) {
                if (allowAdditional) return
                if (source == null) {
                    gameTestHelper.failC("Expected no observations at $attributes for $name, but received preferred of $double and $long")
                } else {
                    gameTestHelper.failC("Expected no observations at $attributes for $name, but received preferred of $double and $long from $source")
                }
            }
            observer.observePreferred(double, long, attributes, source)
        }

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val observer = subObservers.getOrElse(attributes) {
                if (allowAdditional) return
                if (source == null) {
                    gameTestHelper.failC("Expected no observations at $attributes for $name, but received long of $value")
                } else {
                    gameTestHelper.failC("Expected no observations at $attributes for $name, but received long of $value from $source")
                }
            }
            observer.observe(value, attributes, source)
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            val observer = subObservers.getOrElse(attributes) {
                if (allowAdditional) return
                if (source == null) {
                    gameTestHelper.failC("Expected no observations at $attributes for $name, but received double of $value")
                } else {
                    gameTestHelper.failC("Expected no observations at $attributes for $name, but received double of $value from $source")
                }
            }
            observer.observe(value, attributes, source)
        }
    }
}
