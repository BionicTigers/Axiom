package io.github.bionictigers.axiom.core.scheduler

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.GenericCommand
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesInitial
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesUpdate
import io.github.bionictigers.axiom.core.web.serializable.SchedulerDetails
import io.github.bionictigers.axiom.core.web.serializable.SchedulerOrder
import io.github.bionictigers.axiom.core.web.serializable.StateUpdate
import org.firstinspires.ftc.robotcore.external.Telemetry
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.measureTime

object Scheduler {
    val tick: Long
        get() = SchedulerState.tick

    var telemetry: Telemetry?
        set(value) {
            SchedulerState.telemetry = value
        }
        get() = SchedulerState.telemetry
    
    init {
        Server.onNewConnection { send ->
            val serialized = DeltaResolver.serialize(SchedulerState.commands, SchedulerState.systems)
            send(SchedulablesInitial(serialized))
            send(SchedulerOrder(SchedulerState.sortedCommands.map { it.id }))
        }
    }

    fun schedule(vararg command: Command<*>) = command.forEach(::schedule)
    @JvmName("scheduleCommands")
    fun schedule(commands: Collection<Command<*>>) = commands.forEach(::schedule)
    fun schedule(command: Command<*>) {
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.addQueue.add(command)
        } else {
            SchedulerState.add(command)
        }
    }

    fun schedule(vararg system: System) = system.forEach(::schedule)
    @JvmName("scheduleSystems")
    fun schedule(systems: Collection<System>) = systems.forEach(::schedule)
    fun schedule(system: System) {
        SchedulerState.addSystem(system)
        system.update?.parent = system
        system.apply?.parent = system
        schedule(listOfNotNull(system.update, system.apply))
    }

    fun unschedule(vararg command: Command<*>) = command.forEach(::unschedule)
    fun unschedule(commands: Collection<Command<*>>) = commands.forEach(::unschedule)
    fun unschedule(command: Command<*>) {
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.removeQueue.add(command)
        } else {
            SchedulerState.remove(command)
        }
    }

    fun edit(path: String, value: String) {
        RobotLog.dd("Axiom", "Edit: $path = $value")
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.editQueue.add(path to value)
        } else {
            PropertyEditor.edit(path, value)
        }
    }

    /**
     * Clear the scheduler state.
     */
    fun clear() {
        SchedulerState.clear()
    }

    fun tick() {
        SchedulerState.inUpdateCycle = true

        try {
            SchedulerState.deltaTime = measureTime {
                SchedulerState.processEditQueue()
                SchedulerState.processAddQueue()

                if (SchedulerState.changed) {
                    SchedulerState.sortedCommands = DependencyResolver.sort(SchedulerState.commands)
                    SchedulerState.changed = false

                    try {
                        Server.send(SchedulerOrder(SchedulerState.sortedCommands.map { it.id }))
                    } catch (e: Exception) {
                        // Ignore server errors
                    }
                }

                SchedulerState.sortedCommands.forEach(GenericCommand::execute)

                SchedulerState.processRemoveQueue()
            }

            // Serialization & Deltas
            val serialized = DeltaResolver.serialize(SchedulerState.commands, SchedulerState.systems)
            val deltaReport = DeltaResolver.resolve(serialized)

            if (deltaReport.structureUpdates.isNotEmpty() || deltaReport.removals.isNotEmpty()) {
                try {
                    Server.send(SchedulablesUpdate(deltaReport.structureUpdates, deltaReport.removals))
                } catch (e: Exception) {
                    // Ignore server errors
                }
            }

            if (deltaReport.stateUpdates.isNotEmpty()) {
                try {
                    Server.send(StateUpdate(deltaReport.stateUpdates))
                } catch (e: Exception) {
                    // Ignore server errors
                }
            }
        } finally {
            SchedulerState.tick++
            SchedulerState.inUpdateCycle = false

            Server.send(
                SchedulerDetails(
                    SchedulerState.tick,
                    SchedulerState.deltaTime.toDouble(DurationUnit.SECONDS),
                    java.lang.System.currentTimeMillis() / 1000.0
                )
            )
        }
    }
}
