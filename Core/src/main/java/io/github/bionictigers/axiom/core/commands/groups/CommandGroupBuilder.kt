package io.github.bionictigers.axiom.core.commands.groups

import io.github.bionictigers.axiom.core.commands.Command
import kotlin.time.Duration

@DslMarker
annotation class CommandGroupDsl

class CommandGroupBuilder {
    val commands = mutableListOf<Command<*>>()

    fun add(command: Command<*>) {
        commands.add(command)
    }

    fun instant(name: String? = null, block: (Command.Meta) -> Unit) {
        add(Command.instant(name ?: "Instant Command", block))
    }

    fun continuous(name: String? = null, block: (Command.Meta) -> Unit) {
        add(Command.continuous(name ?: "Continuous Command", action = block))
    }

    fun wait(duration: Duration, name: String? = null) {
        add(Command.wait(name ?: "Wait Command", duration))
    }

    fun waitUntil(name: String? = null, predicate: (Command.Meta) -> Boolean) {
        add(Command.create(name ?: "Wait Until") {
            requires {
                predicate(it)
            }

            action { true }
        })
    }
}