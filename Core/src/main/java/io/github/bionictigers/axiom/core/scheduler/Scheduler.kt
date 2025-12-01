package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.GenericCommand
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.web.Server

object Scheduler {
    init {
        Server.onNewConnection { send ->

        }
    }

    fun schedule(vararg command: Command<*>) = command.forEach(::schedule)
    fun schedule(commands: Collection<Command<*>>) = commands.forEach(::schedule)
    fun schedule(command: Command<*>) {
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.addQueue.add(command)
        } else {
            SchedulerState.add(command)
        }
    }

    fun schedule(vararg system: System) = system.forEach(::schedule)
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

    fun tick() {
        SchedulerState.inUpdateCycle = true

        SchedulerState.processAddQueue()

        if (SchedulerState.changed) {
            SchedulerState.sortedCommands = DependencyResolver.sort(SchedulerState.commands)
            SchedulerState.changed = false
        }

        SchedulerState.sortedCommands.forEach(GenericCommand::execute)

        SchedulerState.processRemoveQueue()

        SchedulerState.tick++
        SchedulerState.inUpdateCycle = false
    }
}