package io.github.bionictigers.axiom.core.commands

import java.util.UUID

/**
 * A system is the base class for all systems/mechanisms.
 * Examples include Gamepad, Drivetrain, and Intake.
 *
 * All commands should be associated with at least one system.
 *
 * @see Scheduler
 * @see Command
 */
abstract class System {
    abstract val name: String
    val id: String = UUID.randomUUID().toString()

    val dependencies: List<System> = emptyList()

    val beforeRun: Command<out BaseCommandState>? = null
    val afterRun: Command<out BaseCommandState>? = null
}
