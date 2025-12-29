package com.qualcomm.robotcore.eventloop.opmode

class OpModeManagerImpl {
    var activeOpMode: OpMode? = null
    var activeOpModeName: String? = null
    fun registerListener(listener: OpModeManagerNotifier.Notifications) {}
    fun unregisterListener(listener: OpModeManagerNotifier.Notifications) {}
    fun initOpMode(opModeName: String?) {}
    fun startActiveOpMode() {}
    fun stopActiveOpMode() {}
}