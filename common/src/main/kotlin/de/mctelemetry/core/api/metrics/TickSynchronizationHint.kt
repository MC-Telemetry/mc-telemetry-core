package de.mctelemetry.core.api.metrics

/**
 * **Feature not implemented yet. ALL values behave like [TickSynchronizationHint.NONE].**
 *
 * Specifies whether invocations of the callback can run parallel to the running server or if they have to be
 * synchronized to run after the current tick. If both tick-synchronization invocation-synchronization are enabled,
 * all invocations of the callback are guaranteed to run sequentially on the minecraft tick thread.
 *
 * Unless set to [TickSynchronizationHint.REQUIRED], parallelization of multiple invocations can be overridden by
 * the user, with [TickSynchronizationHint.NONE] and [TickSynchronizationHint.RECOMMENDED] providing various
 * defaults.
 *
 * @see InvocationSynchronizationHint
 */
//TODO: Implement tick synchronization
enum class TickSynchronizationHint(val recommended: Boolean, val required: Boolean) {

    /**
     * **Feature not implemented yet. ALL values behave like [TickSynchronizationHint.NONE].**
     *
     * Callback will be run in parallel to the minecraft server, unless overridden by the user.
     *
     * Callbacks MUST produce the same data regardless of progress in the current server-tick and whether a server-tick
     * is even being processed.
     */
    NONE(false, false),

    /**
     * **Feature not implemented yet. ALL values behave like [TickSynchronizationHint.NONE].**
     *
     * Callback will be run during a minecraft server tick (which will be blocked during metric collection),
     * unless overridden by the user.
     *
     * Callbacks MAY produce inconsistent data depending on progress in the current server-tick when invoked on a
     * different thread, and whether a server-tick is even concurrently being processed, but still have to complete
     * without throwing exceptions.
     *
     * This does not control parallelization of callback-invocations among themselves,
     * see [InvocationSynchronizationHint] for that. This also means that unless combined with
     * [InvocationSynchronizationHint], some invocations of the callback may run on different threads than the
     * minecraft server.
     *
     * @see InvocationSynchronizationHint
     */
    RECOMMENDED(true, false),

    /**
     * **Feature not implemented yet. ALL values behave like [TickSynchronizationHint.NONE].**
     *
     * Callback will always be run during a minecraft server tick (which will be blocked during metric collection).
     *
     * Callbacks MAY throw exceptions, record partial and/or bad data when invoked currently to a server-tick
     * (which they never will be).
     *
     * This does not control parallelization of callback-invocations among themselves,
     * see [InvocationSynchronizationHint] for that. This also means that unless combined with
     * [InvocationSynchronizationHint], some invocations of the callback may run on different threads than the
     * minecraft server.
     */
    REQUIRED(true, true),
    ;

    companion object {

        val DEFAULT = RECOMMENDED
    }
}
