package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.GenericCommand
import io.github.bionictigers.axiom.core.commands.System
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

internal object SchedulerState {
    val commands = ConcurrentHashMap<String, GenericCommand>()
    var sortedCommands = listOf<GenericCommand>()
    val systems = ConcurrentHashMap<String, System>()
    val persistentStates = ConcurrentHashMap<String, Any>()

    val addQueue = ConcurrentLinkedQueue<GenericCommand>()
    val removeQueue = ConcurrentLinkedQueue<GenericCommand>()
    val editQueue = ConcurrentLinkedQueue<Pair<String, Any>>()

    var tick = 0L
    var changed = false
    var inUpdateCycle = false

    fun addSystem(system: System) {
        systems[system.id] = system
    }

    fun add(command: GenericCommand) {
        changed = true
        commands[command.id] = command
        command.schedulerEnter()
    }

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
}