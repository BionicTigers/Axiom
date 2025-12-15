package io.github.bionictigers.axiom.core.commands

import io.github.bionictigers.axiom.core.scheduler.Scheduler
import io.github.bionictigers.axiom.core.web.Hidden
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.measureTime

private class CommandStopped : RuntimeException(null, null, false, false)

class ExecutableDsl<S>(val command: Command<S>) {
    fun stop(): Nothing {
        Scheduler.unschedule(command)

        throw CommandStopped()
    }

    internal fun action(lambda: ExecutableDsl<S>.(S?, Command.Meta) -> Unit) {
        lambda(command.state, command.meta)
    }

    internal fun predicate(lambda: ExecutableDsl<S>.(S?, Command.Meta) -> Boolean): Boolean {
        return lambda(command.state, command.meta)
    }
}

typealias GenericCommand = Command<*>

/**
 * Commands are used to execute functions in the scheduler.
 *
 * They are made to be fully extensible and can be used to create complex systems.
 *
 * @see Scheduler
 * @see System
 */
@Suppress("unused")
open class Command<S>
internal constructor(
    @Hidden
    val name: String = "Unnamed Command",
    val state: S?,
    private val interval: Duration? = null,
    internal var parent: System? = null
) : Schedulable {
    override val id = UUID.randomUUID().toString()
    val dependencies = ArrayList<WeakReference<Command<*>>>()

    val meta = Meta(TimeSource.Monotonic.markNow(), TimeSource.Monotonic.markNow())

    private var predicate: ExecutableDsl<S>.(S?, Meta) -> Boolean = { _, _ -> true }
    private var action: ExecutableDsl<S>.(S?, Meta) -> Unit = { _, _ -> }

    internal var onEnter: (S?, Meta) -> Unit = { _, _ -> }
    internal var onExit: (S?, Meta) -> Unit = { _, _ -> }

    internal var running = false

    private val executableDsl = ExecutableDsl(this)

    init {
        parent?.let { dependsOn(it) }
    }

    /**
     * Adds systems that the command depends on.
     *
     * @param systems The systems that the command depends on.
     * @see System
     */
    fun dependsOn(vararg systems: System): Command<S> {
        dependencies.addAll(systems.mapNotNull { it.update.toWeakReference() })
        return this
    }

    /**
     * Adds a list of systems that the command depends on.
     *
     * @param systems The list of systems that the command depends on.
     * @see System
     */
    fun dependsOnSystem(systems: List<System>): Command<S> {
        dependencies.addAll(systems.mapNotNull { it.update.toWeakReference() })
        return this
    }

    /**
     * Adds commands that the command depends on.
     *
     * @param commands The commands that the command depends on.
     * @see Command
     */
    fun dependsOn(vararg commands: Command<S>): Command<S> {
        dependencies.addAll(commands.mapNotNull { it.toWeakReference() })
        return this
    }

    /**
     * Adds a list of commands that the command depends on.
     *
     * @param commands The list of commands that the command depends on.
     * @see Command
     */
    fun dependsOn(commands: List<Command<S>>): Command<S> {
        dependencies.addAll(commands.mapNotNull { it.toWeakReference() })
        return this
    }

    /**
     * Assigns a function to be invoked during command execution.
     *
     * @param lambda The function to be invoked. The value returned in the lambda determines if the
     * command stays in the scheduler. Call stop() to remove the command from the scheduler.
     */
    fun action(lambda: ExecutableDsl<S>.(Meta) -> Unit): Command<S> {
        action = { _, meta -> lambda(meta) }
        return this
    }

    /**
     * Assigns a function to be invoked during command execution.
     *
     * @param lambda The function to be invoked. The value returned in the lambda determines if the
     * command stays in the scheduler. Call stop() to remove the command from the scheduler.
     */
    fun action(lambda: ExecutableDsl<S>.(S, Meta) -> Unit): Command<S> {
        checkNotNull(state) {
            "You cannot use stateful action without a state! Try action { meta -> ... }."
        }
        action = { s, meta -> lambda(s!!, meta) }
        return this
    }

    /**
     * Executes the command if the predicate is true. If the predicate is false, the command will be
     * removed from the scheduler.
     *
     * @param lambda The predicate to be invoked. The value returned in the lambda determines if the
     * command should be executed.
     */
    fun requires(lambda: ExecutableDsl<S>.(Meta) -> Boolean): Command<S> {
        predicate = { _, meta -> lambda(meta) }
        return this
    }

    /**
     * Executes the command if the predicate is true. If the predicate is false, the command will be
     * removed from the scheduler.
     *
     * @param lambda The predicate to be invoked. The value returned in the lambda determines if the
     * command should be executed.
     */
    fun requires(lambda: ExecutableDsl<S>.(S, Meta) -> Boolean): Command<S> {
        checkNotNull(state) {
            "You cannot use stateful requires without a state! Try requires { meta -> ... }."
        }
        predicate = { s, meta -> lambda(s!!, meta) }
        return this
    }

    /**
     * Executes when the command is entering the scheduler.
     *
     * @param lambda The function to be invoked.
     */
    fun enter(lambda: (Meta) -> Unit): Command<S> {
        onEnter = { _, meta -> lambda(meta) }
        return this
    }

    /**
     * Executes when the command is entering the scheduler.
     *
     * @param lambda The function to be invoked.
     */
    fun enter(lambda: (S, Meta) -> Unit): Command<S> {
        checkNotNull(state) {
            "You cannot use stateful enter without a state! Try enter { meta -> ... }."
        }
        onEnter = { s, meta -> lambda(s!!, meta) }
        return this
    }

    /**
     * Executes when the command is leaving the scheduler.
     *
     * @param lambda The function to be invoked.
     */
    fun exit(lambda: (Meta) -> Unit): Command<S> {
        onExit = { _, meta -> lambda(meta) }
        return this
    }

    /**
     * Executes when the command is leaving the scheduler.
     *
     * @param lambda The function to be invoked.
     */
    fun exit(lambda: (S, Meta) -> Unit): Command<S> {
        checkNotNull(state) {
            "You cannot use stateful exit without a state! Try exit { meta -> ... }."
        }
        onExit = { s, meta -> lambda(s!!, meta) }
        return this
    }

    /**
     * Executes the command.
     *
     * @return True if the command was executed, false otherwise.
     */
    internal fun execute() {
        meta.deltaTime = meta.lastExecutedAt.elapsedNow()
        if (interval != null && meta.deltaTime < interval) return
        meta.lastExecutedAt = TimeSource.Monotonic.markNow()

        try {
            val time = measureTime {
                if (executableDsl.predicate(predicate)) executableDsl.action(action)
            }

            meta.executionTime = time
        } catch (e: CommandStopped) {
            return
        }
    }

    internal fun schedulerEnter() {
        onEnter(state, meta)
        meta.enteredAt = TimeSource.Monotonic.markNow()
        meta.lastExecutedAt = TimeSource.Monotonic.markNow()
        running = true
    }

    internal fun schedulerExit() {
        onExit(state, meta)
        running = false
    }

    fun copy(name: String? = null, state: S? = null): Command<S> {
        val newCommand = Command(name ?: this.name, state ?: this.state, interval, parent)
        newCommand.predicate = predicate
        newCommand.action = action
        newCommand.onEnter = onEnter
        newCommand.onExit = onExit
        newCommand.dependencies.addAll(dependencies)
        return newCommand
    }

    companion object : CommandBuilder()

    data class Meta(
        @Hidden(false) var enteredAt: TimeMark,
        @Hidden(false) var lastExecutedAt: TimeMark,
        @Hidden(false) var executionTime: Duration = Duration.ZERO,
        @Hidden var deltaTime: Duration = Duration.ZERO,
        @Hidden internal var parent: System? = null,
    )
}

private fun <T> T?.toWeakReference(): WeakReference<T>? {
    if (this == null) return null
    return WeakReference(this)
}