package io.github.bionictigers.axiom.core.commands

import io.github.bionictigers.axiom.core.web.Hidden
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
    @Hidden
    abstract val name: String
    @Hidden
    val id: String = UUID.randomUUID().toString()

    @Hidden
    open val dependencies: List<System> = emptyList()

    open val beforeRun: Command<out BaseCommandState>? = null
    open val afterRun: Command<out BaseCommandState>? = null

    @Suppress("PropertyName")
    val SystemCommand = CommandBuilder(this)
}
