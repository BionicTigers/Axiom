package io.github.bionictigers.axiom.core.commands

import io.github.bionictigers.axiom.core.web.Hidden
import kotlin.time.Duration
import kotlin.time.TimeMark

open class BaseCommandState(
    @Hidden var enteredAt: TimeMark? = null,
    @Hidden var lastExecutedAt: TimeMark? = null,
    var deltaTime: Duration = Duration.ZERO,
) {
    internal fun resetTimings() {
        enteredAt = null
        lastExecutedAt = null
        deltaTime = Duration.ZERO
    }
}