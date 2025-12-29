package com.qualcomm.robotcore.eventloop.opmode

object OpModeManagerNotifier {
    interface Notifications {
        fun onOpModePreInit(opMode: OpMode?)
        fun onOpModePreStart(opMode: OpMode?)
        fun onOpModePostStop(opMode: OpMode?)
    }
}