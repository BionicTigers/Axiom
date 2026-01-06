package io.github.bionictigers.axiom.core.commands.bt.decorators

import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for UntilFail decorator, exposed for Seek debugging.
 */
data class UntilFailState(
    val childName: String,
    var iterations: Int = 0,
    var status: String = BtStatus.RUNNING.name
)

/**
 * UntilFail decorator node.
 *
 * Repeats its child until it fails:
 * - While child returns SUCCESS, repeat it
 * - When child returns FAILURE, return SUCCESS
 * - RUNNING while child is running
 *
 * Useful for "do something until a condition is no longer met" patterns.
 *
 * @param name Name of this until-fail node
 * @param child The child BT node to repeat until it fails
 */
class UntilFail(
    name: String = "UntilFail",
    private val child: BtCommand<*>
) : BtCommand<UntilFailState>(
    name,
    UntilFailState(child.name)
) {
    init {
        enter {
            child.reset()
            child.blackboard = blackboard
            state!!.iterations = 0
            state.status = BtStatus.RUNNING.name
            Scheduler.schedule(child)
        }

        action { state, _ ->
            if (!child.running) {
                when (child.status) {
                    BtStatus.SUCCESS -> {
                        // Child succeeded, repeat it
                        state.iterations++
                        child.reset()
                        Scheduler.schedule(child)
                    }
                    BtStatus.FAILURE -> {
                        // Child failed, we're done (with success)
                        state.status = BtStatus.SUCCESS.name
                        succeed()
                    }
                    BtStatus.RUNNING -> {
                        // Should not happen if !child.running
                    }
                }
            }
        }

        exit {
            if (child.running) {
                Scheduler.unschedule(child)
            }
        }
    }

    override fun reset() {
        super.reset()
        state?.iterations = 0
        state?.status = BtStatus.RUNNING.name
        child.reset()
    }
}
