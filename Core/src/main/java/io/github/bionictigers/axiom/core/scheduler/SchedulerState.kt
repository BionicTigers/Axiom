package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.System
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

typealias GenericCommand = Command<*>

internal object SchedulerState {
    val commands = ConcurrentHashMap<String, GenericCommand>()
    val sortedCommands = ArrayList<GenericCommand>()
    val systems = ConcurrentHashMap<String, System>()
    val persistentStates = ConcurrentHashMap<String, Any>()

    val addQueue = ConcurrentLinkedQueue<GenericCommand>()
    val removeQueue = ConcurrentLinkedQueue<GenericCommand>()
    val editQueue = ConcurrentLinkedQueue<Pair<String, Any>>()

    var tick = 0L
    var changed = false
    var inUpdateCycle = false

    fun add(command: GenericCommand) {
        changed = true
        commands[command.id] = command
        command.schedulerEnter()
    }

    fun remove(command: GenericCommand) {
        changed = true
        commands.remove(command.id)
        command.dependencies.forEach { dep ->
            if (dep !in commands.values) {
                return
            }
            dep.dependencies.remove(command)
        }
        command.schedulerExit()
    }
}