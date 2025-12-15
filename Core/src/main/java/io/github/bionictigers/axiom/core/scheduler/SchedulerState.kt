package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.GenericCommand
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.Notification
import com.qualcomm.robotcore.util.RobotLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

internal object SchedulerState {
    val commands = ConcurrentHashMap<String, GenericCommand>()
    var sortedCommands = listOf<GenericCommand>()
    val systems = ConcurrentHashMap<String, System>()
    val persistentStates = ConcurrentHashMap<String, Any>()

    val addQueue = ConcurrentLinkedQueue<GenericCommand>()
    val removeQueue = ConcurrentLinkedQueue<GenericCommand>()
    val editQueue = ConcurrentLinkedQueue<Pair<String, String>>()

    var tick = 0L
    var deltaTime = Duration.ZERO

    var changed = false
    var inUpdateCycle = false

    /**
     * Add a system to the scheduler state.
     *
     * @param system The system to add.
     */
    fun addSystem(system: System) {
        systems[system.id] = system
    }

    /**
     * Add a command to the scheduler state.
     *
     * @param command The command to add.
     */
    fun add(command: GenericCommand) {
        changed = true
        commands[command.id] = command
        command.schedulerEnter()
    }

    /**
     * Remove a command from the scheduler state.
     *
     * @param command The command to remove.
     */
    fun remove(command: GenericCommand) {
        changed = true
        commands.remove(command.id)
        command.schedulerExit()
    }

    fun processAddQueue() {
        while (addQueue.isNotEmpty()) {
            val command = addQueue.poll() ?: continue
            add(command)
        }
    }

    fun processRemoveQueue() {
        while (removeQueue.isNotEmpty()) {
            val command = removeQueue.poll() ?: continue
            remove(command)
        }
    }

    fun processEditQueue() {
        while (editQueue.isNotEmpty()) {
            val (path, value) = editQueue.poll() ?: continue
            try {
                PropertyEditor.edit(path, value)
            } catch (e: Exception) {
                RobotLog.ww("Axiom", e, "Failed to process edit for path: $path")
                Server.send(Notification(
                    "Edit Failure",
                    "Failed to process edit for path: $path\nPlease resynchronize.",
                    Notification.Type.WARNING
                ))
            }
        }
    }
}