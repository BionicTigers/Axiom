package io.github.bionictigers.axiom.core.commands

import io.github.bionictigers.axiom.core.web.Hidden

/**
 * A system is the base class for all systems/mechanisms.
 * Examples include Gamepad, Drivetrain, and Intake.
 *
 * All commands should be associated with at least one system.
 *
 * @see io.github.bionictigers.axiom.core.scheduler.Scheduler
 * @see Command
 */
abstract class System : Schedulable {
    @Hidden
    abstract val name: String
    @Hidden
    override val id: String = javaClass.name.hashCode().toUInt().toString(16)

    @Hidden
    open val dependencies: List<System> = emptyList()

    open val update: Command<*>? = null
    open val apply: Command<*>? = null

    @Suppress("PropertyName")
    val SystemCommand = CommandBuilder(this)
}
