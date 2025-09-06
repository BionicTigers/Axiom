package io.github.bionictigers.axiom.core.commands

import android.annotation.TargetApi
import android.os.Build
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

typealias BaseCommand = Command<BaseCommandState>

private class CommandStopped : RuntimeException(null, null, false, false)

class ExecutableDsl<T: BaseCommandState>(val command: Command<T>) {
    fun stop(): Nothing {
        Scheduler.remove(command)

        throw CommandStopped()
    }

    internal fun action(lambda: ExecutableDsl<T>.(T) -> Unit) {
        lambda(command.state)
    }

    internal fun predicate(lambda: ExecutableDsl<T>.(T) -> Boolean): Boolean {
        return lambda(command.state)
    }
}

/**
 * Commands are used to execute functions in the scheduler.
 *
 * They are made to be fully extensible and can be used to create complex systems.
 *
 * @see Scheduler
 * @see System
 */
@Suppress("unused")
open class Command<T: BaseCommandState> internal constructor(
    val name: String = "Unnamed Command",
    val state: T,
    private val interval: Duration? = null,
    internal var parent: System? = null
) {
    val dependencies = ArrayList<Command<out BaseCommandState>>()
    val id = UUID.randomUUID().toString()

    private var predicate: ExecutableDsl<T>.(T) -> Boolean = { true }
    private var action: ExecutableDsl<T>.(T) -> Unit = {}

    internal var onEnter: (T) -> Unit = {}
    internal var onExit: (T) -> Unit = {}

    internal var running = false

    private val executableDsl = ExecutableDsl<T>(this)

    init {
        parent?.let { dependsOn(it) }
    }

    /**
     * Adds systems that the command depends on.
     *
     * @param systems The systems that the command depends on.
     * @see System
     */
    fun dependsOn(vararg systems: System): Command<T> {
        dependencies.addAll(systems.mapNotNull { it.beforeRun })
        return this
    }

    /**
     * Adds a list of systems that the command depends on.
     *
     * @param systems The list of systems that the command depends on.
     * @see System
     */
    fun dependsOnSystem(systems: List<System>): Command<T> {
        dependencies.addAll(systems.mapNotNull { it.beforeRun })
        return this
    }

    /**
     * Adds commands that the command depends on.
     *
     * @param commands The commands that the command depends on.
     * @see Command
     */
    fun dependsOn(vararg commands: Command<BaseCommandState>): Command<T> {
        dependencies.addAll(commands)
        return this
    }

    /**
     * Adds a list of commands that the command depends on.
     *
     * @param commands The list of commands that the command depends on.
     * @see Command
     */
    fun dependsOn(commands: List<Command<BaseCommandState>>): Command<T> {
        dependencies.addAll(commands)
        return this
    }

    /**
     * Assigns a function to be invoked during command execution.
     *
     * @param lambda The function to be invoked. The value returned in the lambda determines if the command stays in the scheduler. True means it leaves the scheduler.
     */
    fun action(lambda: ExecutableDsl<T>.(T) -> Unit): Command<T> {
        action = lambda
        return this
    }

    /**
     * Executes the command if the predicate is true.
     * If the predicate is false, the command will be removed from the scheduler.
     *
     * @param lambda The predicate to be invoked. The value returned in the lambda determines if the command should be executed.
     */
    fun requires(lambda: ExecutableDsl<T>.(T) -> Boolean): Command<T> {
        predicate = lambda
        return this
    }

    /**
     * Executes when the command is entering the scheduler.
     *
     * @param lambda The function to be invoked.
     */
    fun enter(lambda: (T) -> Unit): Command<T> {
        onEnter = lambda
        return this
    }

    /**
     * Executes when the command is leaving the scheduler.
     *
     * @param lambda The function to be invoked.
     */
    fun exit(lambda: (T) -> Unit): Command<T> {
        onExit = lambda
        return this
    }

    /**
     * Executes the command.
     *
     * @return True if the command was executed, false otherwise.
     */
    internal fun execute() {
        state.deltaTime = state.lastExecutedAt?.elapsedNow() ?: Duration.ZERO
        if (interval != null && state.deltaTime < interval) return
        state.lastExecutedAt = TimeSource.Monotonic.markNow()

        try {
            val time = measureTime {
                if (executableDsl.predicate(predicate))
                    executableDsl.action(action)
            }

            state.executionTime = time
        } catch (e: CommandStopped) {
            return
        }
    }

    internal fun internalEnter() {
        onEnter(state)
        state.enteredAt = TimeSource.Monotonic.markNow()
        running = true
    }

    internal fun internalExit() {
        onExit(state)
        state.resetTimings()
        running = false
    }

    fun copy(name: String? = null, state: T? = null): Command<T> {
        val newCommand = Command(name ?: this.name, state ?: this.state, interval, parent)
        newCommand.predicate = predicate
        newCommand.action = action
        newCommand.onEnter = onEnter
        newCommand.onExit = onExit
        newCommand.dependencies.addAll(dependencies)
        return newCommand
    }

    companion object : CommandBuilder()
}
