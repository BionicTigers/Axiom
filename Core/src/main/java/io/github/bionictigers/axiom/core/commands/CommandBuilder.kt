package io.github.bionictigers.axiom.core.commands

import kotlin.time.Duration

typealias SimpleCommand = Command<Nothing>

open class CommandBuilder(val parent: System? = null) {
    /**
     * Creates a command without state that can be configured with the DSL.
     */
    fun create(
        name: String = "Unnamed Command",
        interval: Duration? = null,
        block: SimpleCommand.() -> Unit = {}
    ): SimpleCommand {
        return Command(name, null, interval, parent).apply(block)
    }

    /**
     * Creates a command with custom state that can be configured with the DSL.
     */
    fun <S> create(
        name: String = "Unnamed Command",
        state: S,
        interval: Duration? = null,
        block: Command<S>.() -> Unit = {}
    ): Command<S> {
        return Command(name, state, interval, parent).apply(block)
    }

    /**
     * Creates a continuous command (runs until stopped) without state.
     * The action receives metadata and should not return a value.
     */
    fun continuous(
        name: String = "Continuous Command",
        interval: Duration? = null,
        action: (Command.Meta) -> Unit
    ): SimpleCommand {
        return Command<Nothing>(name, null, interval, parent).action { meta ->
            action(meta)
        }
    }

    /**
     * Creates a continuous command (runs until stopped) with custom state.
     * The action receives state and metadata and should not return a value.
     */
    fun <S> continuous(
        name: String = "Continuous Command",
        state: S,
        interval: Duration? = null,
        action: (S, Command.Meta) -> Unit
    ): Command<S> {
        return Command(name, state, interval, parent).action { s, meta ->
            action(s, meta)
        }
    }

    /**
     * Creates an instant command (executes once then removes itself) without state.
     */
    fun instant(
        name: String = "Instant command",
        actionToRun: (Command.Meta) -> Unit
    ): SimpleCommand {
        return Command<Nothing>(name, null, null, parent).action { meta ->
            actionToRun(meta)
            stop()
        }
    }

    /**
     * Creates an instant command (executes once then removes itself) with custom state.
     */
    fun <S> instant(
        name: String = "Instant command",
        state: S,
        actionToRun: (S, Command.Meta) -> Unit
    ): Command<S> {
        return Command(name, state, null, parent).action { s, meta ->
            actionToRun(s, meta)
            stop()
        }
    }

    /**
     * Creates a wait command that completes after the specified duration.
     */
    fun wait(name: String = "Wait Command", duration: Duration): SimpleCommand {
        return create(name) {
            action { meta ->
                if (meta.enteredAt.elapsedNow() >= duration) {
                    stop()
                }
            }
        }
    }
}