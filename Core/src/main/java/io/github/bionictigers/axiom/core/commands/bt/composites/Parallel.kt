package io.github.bionictigers.axiom.core.commands.bt.composites

import io.github.bionictigers.axiom.core.commands.bt.Blackboard
import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * Policy for when a Parallel node should succeed or fail.
 */
enum class ParallelPolicy {
    /** Succeed when ALL children succeed, fail when ANY child fails */
    REQUIRE_ALL,
    /** Succeed when ANY child succeeds, fail when ALL children fail */
    REQUIRE_ONE
}

/**
 * State for Parallel composite, exposed for Seek debugging.
 */
data class ParallelState(
    val childNames: List<String>,
    val policy: String,
    var successCount: Int = 0,
    var failureCount: Int = 0,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Parallel composite node.
 *
 * Runs all children concurrently with configurable success/failure policy:
 *
 * [ParallelPolicy.REQUIRE_ALL] (default):
 * - SUCCESS when all children succeed
 * - FAILURE when any child fails
 *
 * [ParallelPolicy.REQUIRE_ONE]:
 * - SUCCESS when any child succeeds
 * - FAILURE when all children fail
 *
 * @param name Name of this parallel node
 * @param children The child BT nodes to run concurrently
 * @param policy The success/failure policy
 */
class Parallel(
    name: String = "Parallel",
    private val children: List<BtCommand<*>>,
    private val policy: ParallelPolicy = ParallelPolicy.REQUIRE_ALL
) : BtCommand<ParallelState>(
    name,
    ParallelState(children.map { it.name }, policy.name)
) {
    init {
        require(children.isNotEmpty()) { "Parallel must have at least one child" }

        enter {
            // Reset and schedule all children
            children.forEach { child ->
                child.reset()
                child.blackboard = blackboard
                Scheduler.schedule(child)
            }
            state!!.successCount = 0
            state.failureCount = 0
            state.status = BtStatus.RUNNING.name
        }

        action { state, _ ->
            var successCount = 0
            var failureCount = 0
            var runningCount = 0

            children.forEach { child ->
                when {
                    child.running -> runningCount++
                    child.status == BtStatus.SUCCESS -> successCount++
                    child.status == BtStatus.FAILURE -> failureCount++
                }
            }

            state.successCount = successCount
            state.failureCount = failureCount

            when (policy) {
                ParallelPolicy.REQUIRE_ALL -> {
                    when {
                        failureCount > 0 -> {
                            // Any failure means parallel fails
                            state.status = BtStatus.FAILURE.name
                            cancelRunningChildren()
                            fail()
                        }
                        successCount == children.size -> {
                            // All succeeded
                            state.status = BtStatus.SUCCESS.name
                            succeed()
                        }
                        // else: still running
                    }
                }
                ParallelPolicy.REQUIRE_ONE -> {
                    when {
                        successCount > 0 -> {
                            // Any success means parallel succeeds
                            state.status = BtStatus.SUCCESS.name
                            cancelRunningChildren()
                            succeed()
                        }
                        failureCount == children.size -> {
                            // All failed
                            state.status = BtStatus.FAILURE.name
                            fail()
                        }
                        // else: still running
                    }
                }
            }
        }

        exit {
            // Clean up: unschedule any still-running children
            cancelRunningChildren()
        }
    }

    private fun cancelRunningChildren() {
        children.forEach { child ->
            if (child.running) {
                Scheduler.unschedule(child)
            }
        }
    }

    override fun reset() {
        super.reset()
        state?.successCount = 0
        state?.failureCount = 0
        state?.status = BtStatus.RUNNING.name
        children.forEach { it.reset() }
    }
}
