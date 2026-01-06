package io.github.bionictigers.axiom.core.commands.bt.decorators

import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for Succeeder decorator, exposed for Seek debugging.
 */
data class SucceederState(
    val childName: String,
    var childStatus: String = BtStatus.RUNNING.name,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Succeeder decorator node.
 *
 * Always returns SUCCESS regardless of child result:
 * - SUCCESS when child completes (regardless of child's actual status)
 * - RUNNING while child is running
 *
 * Useful for optional tasks that shouldn't affect the parent's result.
 *
 * @param name Name of this succeeder node
 * @param child The child BT node
 */
class Succeeder(
    name: String = "Succeeder",
    private val child: BtCommand<*>
) : BtCommand<SucceederState>(
    name,
    SucceederState(child.name)
) {
    init {
        enter {
            child.reset()
            child.blackboard = blackboard
            state!!.childStatus = BtStatus.RUNNING.name
            state.status = BtStatus.RUNNING.name
            Scheduler.schedule(child)
        }

        action { state, _ ->
            if (!child.running) {
                state.childStatus = child.status.name
                state.status = BtStatus.SUCCESS.name
                succeed()
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
        state?.childStatus = BtStatus.RUNNING.name
        state?.status = BtStatus.RUNNING.name
        child.reset()
    }
}
