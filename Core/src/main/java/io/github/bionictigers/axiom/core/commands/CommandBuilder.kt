package io.github.bionictigers.axiom.core.commands

import kotlin.time.Duration

open class CommandBuilder(val parent: System? = null) {
    fun create(name: String = "Unnamed Command", interval: Duration? = null, block: BaseCommand.() -> Unit = {}): BaseCommand {
        return create(name, BaseCommandState(), interval, block)
    }

    fun <T: BaseCommandState> create(name: String = "Unnamed Command", state: T, interval: Duration? = null, block: Command<T>.() -> Unit = {}): Command<T> {
        return Command(name, state, interval, parent).apply(block)
    }

    fun continuous(name: String = "Continuous Command", interval: Duration? = null, action: (BaseCommandState) -> Unit): BaseCommand {
        return continuous(name, BaseCommandState(), interval, action)
    }

    fun <T: BaseCommandState> continuous(name: String = "Continuous Command", state: T, interval: Duration? = null, action: (T) -> Unit): Command<T> {
        return Command(name, state, interval).action { action(it); false }
    }

    fun instant(name: String = "Instant command", actionToRun: (BaseCommandState) -> Unit): BaseCommand {
        return instant(name, BaseCommandState(), actionToRun)
    }

    fun <T: BaseCommandState> instant(name: String = "Instant command", state: T, actionToRun: (T) -> Unit): Command<T> {
        return Command(name, state, parent = parent).action { actionToRun(it); true }
    }

    fun wait(name: String = "Wait Command", duration: Duration): BaseCommand {
        return create(name) { action { it.enteredAt!!.elapsedNow() >= duration } }
    }
}