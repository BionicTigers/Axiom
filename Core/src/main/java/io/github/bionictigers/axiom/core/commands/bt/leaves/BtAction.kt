package io.github.bionictigers.axiom.core.commands.bt.leaves

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.bt.Blackboard
import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for BtAction leaf, exposed for Seek debugging.
 */
data class BtActionState(
    var status: String = BtStatus.RUNNING.name
)

/**
 * DSL context for BtAction, providing access to succeed/fail/blackboard.
 */
class BtActionContext(
    val blackboard: Blackboard?,
    val meta: Command.Meta,
    private val action: BtAction
) {
    /**
     * Complete the action with SUCCESS.
     */
    fun succeed(): Nothing {
        action.completeWithSuccess()
        throw ActionCompleted()
    }

    /**
     * Complete the action with FAILURE.
     */
    fun fail(): Nothing {
        action.completeWithFailure()
        throw ActionCompleted()
    }
}

internal class ActionCompleted : RuntimeException(null, null, false, false)

/**
 * BtAction leaf node.
 *
 * Executes work and completes with SUCCESS or FAILURE:
 * - Call `succeed()` in the action to complete with SUCCESS
 * - Call `fail()` in the action to complete with FAILURE
 * - If the action doesn't call succeed/fail, it stays RUNNING
 *
 * For instant actions that always succeed:
 * ```kotlin
 * BtAction("DoSomething") { 
 *     doWork()
 *     succeed() 
 * }
 * ```
 *
 * For conditional actions:
 * ```kotlin
 * BtAction("TryAcquire") {
 *     if (sensor.detected()) {
 *         blackboard["target"] = sensor.position
 *         succeed()
 *     } else {
 *         fail()
 *     }
 * }
 * ```
 *
 * For async actions that run over multiple ticks:
 * ```kotlin
 * BtAction("WaitForCondition") {
 *     if (conditionMet()) succeed()
 *     // Otherwise stays RUNNING until next tick
 * }
 * ```
 *
 * @param name Name of this action node
 * @param work The work to execute each tick
 */
class BtAction(
    name: String = "Action",
    private val work: BtActionContext.() -> Unit
) : BtCommand<BtActionState>(
    name,
    BtActionState()
) {
    init {
        enter {
            state!!.status = BtStatus.RUNNING.name
        }

        action { _, meta ->
            val context = BtActionContext(blackboard, meta, this@BtAction)
            try {
                context.work()
            } catch (_: ActionCompleted) {
                // Action called succeed() or fail()
            }
        }
    }

    internal fun completeWithSuccess() {
        state?.status = BtStatus.SUCCESS.name
        status = BtStatus.SUCCESS
        Scheduler.unschedule(this)
    }

    internal fun completeWithFailure() {
        state?.status = BtStatus.FAILURE.name
        status = BtStatus.FAILURE
        Scheduler.unschedule(this)
    }

    override fun reset() {
        super.reset()
        state?.status = BtStatus.RUNNING.name
    }
}

/**
 * Create an instant action that always succeeds after executing work.
 *
 * @param name Name of the action
 * @param work The work to execute (blackboard available)
 */
fun instantAction(
    name: String = "InstantAction",
    work: (Blackboard?) -> Unit
): BtAction = BtAction(name) {
    work(blackboard)
    succeed()
}

/**
 * Create an instant action that succeeds or fails based on a result.
 *
 * @param name Name of the action
 * @param work The work to execute, returning true for SUCCESS, false for FAILURE
 */
fun conditionalAction(
    name: String = "ConditionalAction",
    work: (Blackboard?) -> Boolean
): BtAction = BtAction(name) {
    if (work(blackboard)) succeed() else fail()
}
