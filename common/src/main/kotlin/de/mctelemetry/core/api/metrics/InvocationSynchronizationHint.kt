package de.mctelemetry.core.api.metrics

/**
 * **Feature not implemented yet. ALL values behave like [InvocationSynchronizationHint.REQUIRED].**
 *
 * Specifies the parallelizability of an instrument callback amongst itself.
 *
 * Unless set to [InvocationSynchronizationHint.REQUIRED], parallelization of multiple invocations can be overridden by
 * the user, with [InvocationSynchronizationHint.NONE] and [InvocationSynchronizationHint.RECOMMENDED] providing various
 * defaults.
 *
 * @see TickSynchronizationHint
 */
//TODO: Implement invocation synchronization
enum class InvocationSynchronizationHint(val recommended: Boolean, val required: Boolean) {

    /**
     * **Feature not implemented yet. ALL values behave like [InvocationSynchronizationHint.REQUIRED].**
     *
     * Parallelization of the callback is fully supported and enabled unless overridden.
     *
     * Callbacks MUST produce the same data regardless of invocation order.
     */
    NONE(false, false),

    /**
     * **Feature not implemented yet. ALL values behave like [InvocationSynchronizationHint.REQUIRED].**
     *
     * Parallelization of the callback can be enabled by users, but is disabled by default.
     *
     * Callbacks MAY return bad data when invoked in parallel but MUST complete successfully.
     */
    RECOMMENDED(true, false),

    /**
     * **Feature not implemented yet. ALL values behave like [InvocationSynchronizationHint.REQUIRED].**
     *
     * Parallelization of the callback is not supported and cannot be enabled.
     *
     * Callbacks MAY throw exceptions, record partial and/or bad data when invoked in parallel
     * (which they will never be).
     */
    REQUIRED(true, true),
    ;

    companion object {

        val DEFAULT = REQUIRED
    }
}
