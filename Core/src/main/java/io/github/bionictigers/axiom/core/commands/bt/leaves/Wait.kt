package io.github.bionictigers.axiom.core.commands.bt.leaves

import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * State for Wait leaf, exposed for Seek debugging.
 */
data class WaitState(
    val durationMs: Long,
    var elapsedMs: Long = 0,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Wait leaf node.
 *
 * Stays RUNNING for a specified duration, then returns SUCCESS.
 *
 * @param name Name of this wait node
 * @param duration How long to wait
 */
class Wait(
    name: String = "Wait",
    private val duration: Duration
) : BtCommand<WaitState>(
    name,
    WaitState(duration.inWholeMilliseconds)
) {
    private var startTime: TimeSource.Monotonic.ValueTimeMark? = null

    init {
        enter {
            startTime = TimeSource.Monotonic.markNow()
            state!!.elapsedMs = 0
            state.status = BtStatus.RUNNING.name
        }

        action { state, _ ->
            val elapsed = startTime?.elapsedNow() ?: Duration.ZERO
            state.elapsedMs = elapsed.inWholeMilliseconds

            if (elapsed >= duration) {
                state.status = BtStatus.SUCCESS.name
                succeed()
            }
        }
    }

    override fun reset() {
        super.reset()
        startTime = null
        state?.elapsedMs = 0
        state?.status = BtStatus.RUNNING.name
    }
}
