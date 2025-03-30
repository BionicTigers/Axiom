package io.github.bionictigers.axiom.commands

import io.github.bionictigers.axiom.utils.Time
import io.github.bionictigers.axiom.web.Hidden

open class BaseCommandState(
    open val name: String = "Unnamed Command",

    @Hidden var enteredAt: Time = Time(),
    var timeInScheduler: Time = Time(),
    @Hidden var lastExecutedAt: Time = Time(),
    var deltaTime: Time = Time(),
)