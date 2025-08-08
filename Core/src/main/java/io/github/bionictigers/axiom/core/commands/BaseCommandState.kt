package io.github.bionictigers.axiom.core.commands

import io.github.bionictigers.axiom.core.web.Hidden
import kotlin.time.Duration
import kotlin.time.TimeMark

open class BaseCommandState() {
    @Hidden(false) var enteredAt: TimeMark? = null
    @Hidden(false) var lastExecutedAt: TimeMark? = null
    @Hidden(false) var executionTime: Duration = Duration.ZERO
    @Hidden var deltaTime: Duration = Duration.ZERO
    @Hidden internal var parent: System? = null

    internal fun resetTimings() {
        enteredAt = null
        lastExecutedAt = null
        deltaTime = Duration.ZERO
        executionTime = Duration.ZERO
    }
}