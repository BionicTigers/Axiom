package io.github.bionictigers.axiom.core.commands

/**
 * A system is the base class for all systems/mechanisms.
 * Examples include Gamepad, Drivetrain, and Intake.
 *
 * All commands should be associated with at least one system.
 *
 * @see Scheduler
 * @see Command
 */
interface System {
    val name: String

    val dependencies: List<System>?
        get() = emptyList()

    val beforeRun: Command<out BaseCommandState>?
        get() = null
    val afterRun: Command<out BaseCommandState>?
        get() = null
}
