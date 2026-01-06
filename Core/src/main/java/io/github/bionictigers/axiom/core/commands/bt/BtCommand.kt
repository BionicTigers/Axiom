package io.github.bionictigers.axiom.core.commands.bt

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.scheduler.Scheduler
import kotlin.time.Duration

/**
 * Base class for behavior tree nodes.
 *
 * Extends [Command] with BT status semantics (SUCCESS/FAILURE/RUNNING).
 * Use [succeed] or [fail] instead of [stop] to properly signal completion status.
 *
 * Parent composites check [running] and [status] to make decisions.
 */
abstract class BtCommand<S> internal constructor(
    name: String,
    state: S?,
    interval: Duration? = null,
    parent: System? = null
) : Command<S>(name, state, interval, parent) {

    /**
     * The current status of this BT node.
     * Initially [BtStatus.RUNNING] when scheduled.
     */
    var status: BtStatus = BtStatus.RUNNING
        protected set

    /**
     * Reference to the shared blackboard for this behavior tree.
     * Set by the root node or parent composite.
     */
    var blackboard: Blackboard? = null
        internal set

    /**
     * Complete this node with SUCCESS status and unschedule it.
     */
    protected fun succeed() {
        status = BtStatus.SUCCESS
        Scheduler.unschedule(this)
    }

    /**
     * Complete this node with FAILURE status and unschedule it.
     */
    protected fun fail() {
        status = BtStatus.FAILURE
        Scheduler.unschedule(this)
    }

    /**
     * Reset the node status to RUNNING.
     * Called when the node is re-scheduled (e.g., by a Repeater).
     */
    internal open fun reset() {
        status = BtStatus.RUNNING
    }
}
