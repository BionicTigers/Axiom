package io.github.bionictigers.axiom.core.commands.bt

import io.github.bionictigers.axiom.core.commands.bt.composites.Parallel
import io.github.bionictigers.axiom.core.commands.bt.composites.ParallelPolicy
import io.github.bionictigers.axiom.core.commands.bt.composites.Selector
import io.github.bionictigers.axiom.core.commands.bt.composites.Sequence
import io.github.bionictigers.axiom.core.commands.bt.decorators.Inverter
import io.github.bionictigers.axiom.core.commands.bt.decorators.Repeater
import io.github.bionictigers.axiom.core.commands.bt.decorators.Succeeder
import io.github.bionictigers.axiom.core.commands.bt.decorators.UntilFail
import io.github.bionictigers.axiom.core.commands.bt.leaves.BtAction
import io.github.bionictigers.axiom.core.commands.bt.leaves.BtActionContext
import io.github.bionictigers.axiom.core.commands.bt.leaves.Condition
import io.github.bionictigers.axiom.core.commands.bt.leaves.Wait
import kotlin.time.Duration

/**
 * DSL marker to prevent scope leakage in nested builders.
 */
@DslMarker
annotation class BtDsl

/**
 * Result of building a behavior tree.
 * Holds the root node and the shared blackboard.
 */
class BehaviorTree internal constructor(
    val name: String,
    val root: BtCommand<*>,
    val blackboard: Blackboard
) {
    init {
        // Propagate blackboard to root
        root.blackboard = blackboard
    }
}

/**
 * Builder for composite nodes (Selector, Sequence, Parallel).
 * Collects child nodes in the order they're added.
 */
@BtDsl
class BtCompositeBuilder(private val blackboard: Blackboard) {
    internal val children = mutableListOf<BtCommand<*>>()

    /**
     * Add an existing BtCommand as a child.
     */
    fun add(command: BtCommand<*>) {
        command.blackboard = blackboard
        children.add(command)
    }

    // ===== COMPOSITES =====

    /**
     * Add a Selector (fallback) child.
     * Tries children in order until one succeeds.
     */
    fun selector(name: String = "Selector", block: BtCompositeBuilder.() -> Unit) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        val node = Selector(name, builder.children)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add a Sequence child.
     * Runs children in order until one fails.
     */
    fun sequence(name: String = "Sequence", block: BtCompositeBuilder.() -> Unit) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        val node = Sequence(name, builder.children)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add a Parallel child.
     * Runs all children concurrently.
     */
    fun parallel(
        name: String = "Parallel",
        policy: ParallelPolicy = ParallelPolicy.REQUIRE_ALL,
        block: BtCompositeBuilder.() -> Unit
    ) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        val node = Parallel(name, builder.children, policy)
        node.blackboard = blackboard
        children.add(node)
    }

    // ===== DECORATORS =====

    /**
     * Add an Inverter decorator.
     * Inverts the result of its child (SUCCESS ↔ FAILURE).
     */
    fun inverter(name: String = "Inverter", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("Inverter must have exactly one child")
        val node = Inverter(name, child)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add a Repeater decorator.
     * Repeats its child a specified number of times.
     */
    fun repeater(
        name: String = "Repeater",
        times: Int = -1,
        stopOnFailure: Boolean = false,
        block: BtDecoratorBuilder.() -> Unit
    ) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("Repeater must have exactly one child")
        val node = Repeater(name, child, times, stopOnFailure)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add a Succeeder decorator.
     * Always returns SUCCESS regardless of child result.
     */
    fun succeeder(name: String = "Succeeder", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("Succeeder must have exactly one child")
        val node = Succeeder(name, child)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add an UntilFail decorator.
     * Repeats its child until it fails, then returns SUCCESS.
     */
    fun untilFail(name: String = "UntilFail", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("UntilFail must have exactly one child")
        val node = UntilFail(name, child)
        node.blackboard = blackboard
        children.add(node)
    }

    // ===== LEAVES =====

    /**
     * Add a Condition leaf.
     * Immediately succeeds or fails based on the predicate.
     */
    fun condition(name: String = "Condition", predicate: (Blackboard?) -> Boolean) {
        val node = Condition(name, predicate)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add an Action leaf.
     * Executes work; call succeed() or fail() to complete.
     */
    fun action(name: String = "Action", work: BtActionContext.() -> Unit) {
        val node = BtAction(name, work)
        node.blackboard = blackboard
        children.add(node)
    }

    /**
     * Add a Wait leaf.
     * Stays RUNNING for the specified duration, then succeeds.
     */
    fun wait(name: String = "Wait", duration: Duration) {
        val node = Wait(name, duration)
        node.blackboard = blackboard
        children.add(node)
    }
}

/**
 * Builder for decorator nodes (Inverter, Repeater, etc.).
 * Accepts exactly one child.
 */
@BtDsl
class BtDecoratorBuilder(private val blackboard: Blackboard) {
    internal var child: BtCommand<*>? = null

    private fun setChild(node: BtCommand<*>) {
        check(child == null) { "Decorator can only have one child" }
        node.blackboard = blackboard
        child = node
    }

    /**
     * Add an existing BtCommand as the child.
     */
    fun add(command: BtCommand<*>) {
        setChild(command)
    }

    // ===== COMPOSITES =====

    fun selector(name: String = "Selector", block: BtCompositeBuilder.() -> Unit) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        setChild(Selector(name, builder.children))
    }

    fun sequence(name: String = "Sequence", block: BtCompositeBuilder.() -> Unit) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        setChild(Sequence(name, builder.children))
    }

    fun parallel(
        name: String = "Parallel",
        policy: ParallelPolicy = ParallelPolicy.REQUIRE_ALL,
        block: BtCompositeBuilder.() -> Unit
    ) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        setChild(Parallel(name, builder.children, policy))
    }

    // ===== DECORATORS =====

    fun inverter(name: String = "Inverter", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val innerChild = builder.child ?: error("Inverter must have exactly one child")
        setChild(Inverter(name, innerChild))
    }

    fun repeater(
        name: String = "Repeater",
        times: Int = -1,
        stopOnFailure: Boolean = false,
        block: BtDecoratorBuilder.() -> Unit
    ) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val innerChild = builder.child ?: error("Repeater must have exactly one child")
        setChild(Repeater(name, innerChild, times, stopOnFailure))
    }

    fun succeeder(name: String = "Succeeder", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val innerChild = builder.child ?: error("Succeeder must have exactly one child")
        setChild(Succeeder(name, innerChild))
    }

    fun untilFail(name: String = "UntilFail", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val innerChild = builder.child ?: error("UntilFail must have exactly one child")
        setChild(UntilFail(name, innerChild))
    }

    // ===== LEAVES =====

    fun condition(name: String = "Condition", predicate: (Blackboard?) -> Boolean) {
        setChild(Condition(name, predicate))
    }

    fun action(name: String = "Action", work: BtActionContext.() -> Unit) {
        setChild(BtAction(name, work))
    }

    fun wait(name: String = "Wait", duration: Duration) {
        setChild(Wait(name, duration))
    }
}

/**
 * Top-level builder for creating a behavior tree.
 */
@BtDsl
class BtRootBuilder(private val blackboard: Blackboard) {
    internal var root: BtCommand<*>? = null

    private fun setRoot(node: BtCommand<*>) {
        check(root == null) { "Behavior tree can only have one root" }
        node.blackboard = blackboard
        root = node
    }

    // ===== COMPOSITES =====

    fun selector(name: String = "Selector", block: BtCompositeBuilder.() -> Unit) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        setRoot(Selector(name, builder.children))
    }

    fun sequence(name: String = "Sequence", block: BtCompositeBuilder.() -> Unit) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        setRoot(Sequence(name, builder.children))
    }

    fun parallel(
        name: String = "Parallel",
        policy: ParallelPolicy = ParallelPolicy.REQUIRE_ALL,
        block: BtCompositeBuilder.() -> Unit
    ) {
        val builder = BtCompositeBuilder(blackboard)
        builder.block()
        setRoot(Parallel(name, builder.children, policy))
    }

    // ===== DECORATORS =====

    fun inverter(name: String = "Inverter", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("Inverter must have exactly one child")
        setRoot(Inverter(name, child))
    }

    fun repeater(
        name: String = "Repeater",
        times: Int = -1,
        stopOnFailure: Boolean = false,
        block: BtDecoratorBuilder.() -> Unit
    ) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("Repeater must have exactly one child")
        setRoot(Repeater(name, child, times, stopOnFailure))
    }

    fun succeeder(name: String = "Succeeder", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("Succeeder must have exactly one child")
        setRoot(Succeeder(name, child))
    }

    fun untilFail(name: String = "UntilFail", block: BtDecoratorBuilder.() -> Unit) {
        val builder = BtDecoratorBuilder(blackboard)
        builder.block()
        val child = builder.child ?: error("UntilFail must have exactly one child")
        setRoot(UntilFail(name, child))
    }

    // ===== LEAVES =====

    fun condition(name: String = "Condition", predicate: (Blackboard?) -> Boolean) {
        setRoot(Condition(name, predicate))
    }

    fun action(name: String = "Action", work: BtActionContext.() -> Unit) {
        setRoot(BtAction(name, work))
    }

    fun wait(name: String = "Wait", duration: Duration) {
        setRoot(Wait(name, duration))
    }
}

/**
 * Create a behavior tree with the DSL.
 *
 * Example:
 * ```kotlin
 * val tree = behaviorTree("RobotBrain") {
 *     selector("Root") {
 *         sequence("Score") {
 *             condition("HasPiece") { blackboard["hasPiece"] == true }
 *             action("GoToGoal") { drive.goTo(goalPose); succeed() }
 *             action("Place") { outtake.place(); succeed() }
 *         }
 *         sequence("Intake") {
 *             inverter {
 *                 condition("HasPiece") { blackboard["hasPiece"] == true }
 *             }
 *             action("Seek") { /* ... */ }
 *         }
 *         action("Idle") { succeed() }
 *     }
 * }
 *
 * // Schedule the tree root
 * Scheduler.schedule(tree.root)
 *
 * // Access blackboard
 * tree.blackboard["hasPiece"] = true
 * ```
 *
 * @param name Name of the behavior tree
 * @param block DSL block to build the tree
 * @return A [BehaviorTree] containing the root node and shared blackboard
 */
fun behaviorTree(name: String = "BehaviorTree", block: BtRootBuilder.() -> Unit): BehaviorTree {
    val blackboard = Blackboard()
    val builder = BtRootBuilder(blackboard)
    builder.block()
    val root = builder.root ?: error("Behavior tree must have a root node")
    return BehaviorTree(name, root, blackboard)
}

/**
 * Create a behavior tree with an existing blackboard.
 *
 * @param name Name of the behavior tree
 * @param blackboard Existing blackboard to use
 * @param block DSL block to build the tree
 * @return A [BehaviorTree] containing the root node and the provided blackboard
 */
fun behaviorTree(
    name: String = "BehaviorTree",
    blackboard: Blackboard,
    block: BtRootBuilder.() -> Unit
): BehaviorTree {
    val builder = BtRootBuilder(blackboard)
    builder.block()
    val root = builder.root ?: error("Behavior tree must have a root node")
    return BehaviorTree(name, root, blackboard)
}
