package io.github.bionictigers.axiom.core.commands.bt.decorators

import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for Repeater decorator, exposed for Seek debugging.
 */
data class RepeaterState(
    val childName: String,
    val maxIterations: Int,
    var currentIteration: Int = 0,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Repeater decorator node.
 *
 * Repeats its child a specified number of times (or forever):
 * - If [times] is positive, repeat that many times then return SUCCESS
 * - If [times] is <= 0, repeat forever (never completes on its own)
 * - If [stopOnFailure] is true, return FAILURE when child fails
 * - If [stopOnFailure] is false, continue repeating even if child fails
 *
 * @param name Name of this repeater node
 * @param child The child BT node to repeat
 * @param times Number of times to repeat (<=0 means forever)
 * @param stopOnFailure Whether to stop and fail when child fails
 */
class Repeater(
    name: String = "Repeater",
    private val child: BtCommand<*>,
    private val times: Int = -1,
    private val stopOnFailure: Boolean = false
) : BtCommand<RepeaterState>(
    name,
    RepeaterState(child.name, times)
) {
    init {
        enter {
            child.reset()
            child.blackboard = blackboard
            state!!.currentIteration = 0
            state.status = BtStatus.RUNNING.name
            Scheduler.schedule(child)
        }

        action { state, _ ->
            if (!child.running) {
                when (child.status) {
                    BtStatus.SUCCESS -> {
                        state.currentIteration++
                        if (times > 0 && state.currentIteration >= times) {
                            // Completed all iterations
                            state.status = BtStatus.SUCCESS.name
                            succeed()
                        } else {
                            // Repeat
                            child.reset()
                            Scheduler.schedule(child)
                        }
                    }
                    BtStatus.FAILURE -> {
                        if (stopOnFailure) {
                            state.status = BtStatus.FAILURE.name
                            fail()
                        } else {
                            state.currentIteration++
                            if (times > 0 && state.currentIteration >= times) {
                                // Completed all iterations (even though this one failed)
                                state.status = BtStatus.SUCCESS.name
                                succeed()
                            } else {
                                // Repeat despite failure
                                child.reset()
                                Scheduler.schedule(child)
                            }
                        }
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
        state?.currentIteration = 0
        state?.status = BtStatus.RUNNING.name
        child.reset()
    }
}
