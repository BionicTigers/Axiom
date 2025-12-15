package io.github.bionictigers.axiom.core.utils

import io.github.bionictigers.axiom.core.commands.Command
import kotlin.time.Duration

class Timer(val duration: Duration) {
    private var initialTime: Duration? = null
    private var calledOnce = false

    var isFinished = false
        private set

    fun update(state: Command.Meta): Timer {
        val timeInScheduler = state.enteredAt.elapsedNow()

        if (initialTime == null) {
            initialTime = timeInScheduler
        } else if (timeInScheduler - initialTime!! >= duration) {
            isFinished = true
        }

        return this
    }

    fun once(callback: () -> Unit): Timer {
        if (isFinished && !calledOnce) {
            callback()
        }

        return this
    }

    fun finished(callback: () -> Unit): Timer {
        if (isFinished) callback()
        return this
    }

    fun reset() {
        initialTime = null
        isFinished = false
        calledOnce = false
    }
}