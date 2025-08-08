package com.qualcomm.robotcore.util

import java.util.logging.Level
import java.util.logging.Logger

class RobotLog {
    companion object {
        // Define log level constants similar to Android's
        private const val VERBOSE = 2
        private const val DEBUG = 3
        private const val INFO = 4
        private const val WARN = 5
        private const val ERROR = 6
        private const val ASSERT = 7

        val TAG: String = "RobotCore"
        private val logger: Logger = Logger.getLogger(TAG)

        private fun internalLog(priority: Int, tag: String, message: String) {
            // Map our integer priorities to java.util.logging.Level values
            val level = when (priority) {
                VERBOSE -> Level.FINEST
                DEBUG -> Level.FINE
                INFO -> Level.INFO
                WARN -> Level.WARNING
                ERROR, ASSERT -> Level.SEVERE
                else -> Level.INFO
            }
            // Prefix the log message with the tag (as in Android)
            logger.log(level, "[$tag] $message")
            println("[$tag] $message")
        }

        private fun internalLog(priority: Int, tag: String, throwable: Throwable, message: String) {
            internalLog(priority, tag, message)
            logStackTrace(tag, throwable)
        }

        private fun logStackTrace(tag: String, e: Throwable) {
            logger.log(Level.SEVERE, "[$tag] Exception", e)
        }

        //------------------------------------------------------------------------------------------------
        // Logging API
        //------------------------------------------------------------------------------------------------
        fun a(format: String, vararg args: Any) {
            v(String.format(format, *args))
        }

        fun a(message: String) {
            internalLog(ASSERT, TAG, message)
        }

        fun aa(tag: String, format: String, vararg args: Any) {
            vv(tag, String.format(format, *args))
        }

        fun aa(tag: String, message: String) {
            internalLog(ASSERT, tag, message)
        }

        fun aa(tag: String, throwable: Throwable, format: String, vararg args: Any) {
            vv(tag, throwable, String.format(format, *args))
        }

        fun aa(tag: String, throwable: Throwable, message: String) {
            internalLog(ASSERT, tag, throwable, message)
        }

        fun v(format: String, vararg args: Any) {
            v(String.format(format, *args))
        }

        fun v(message: String) {
            internalLog(VERBOSE, TAG, message)
        }

        fun vv(tag: String, format: String, vararg args: Any) {
            vv(tag, String.format(format, *args))
        }

        fun vv(tag: String, message: String) {
            internalLog(VERBOSE, tag, message)
        }

        fun vv(tag: String, throwable: Throwable, format: String, vararg args: Any) {
            vv(tag, throwable, String.format(format, *args))
        }

        fun vv(tag: String, throwable: Throwable, message: String) {
            internalLog(VERBOSE, tag, throwable, message)
        }

        fun d(format: String, vararg args: Any) {
            d(String.format(format, *args))
        }

        fun d(message: String) {
            internalLog(DEBUG, TAG, message)
        }

        fun dd(tag: String, format: String, vararg args: Any) {
            dd(tag, String.format(format, *args))
        }

        fun dd(tag: String, message: String) {
            internalLog(DEBUG, tag, message)
        }

        fun dd(tag: String, throwable: Throwable, format: String, vararg args: Any) {
            dd(tag, throwable, String.format(format, *args))
        }

        fun dd(tag: String, throwable: Throwable, message: String) {
            internalLog(DEBUG, tag, throwable, message)
        }

        fun i(format: String, vararg args: Any) {
            i(String.format(format, *args))
        }

        fun i(message: String) {
            internalLog(INFO, TAG, message)
        }

        fun ii(tag: String, format: String, vararg args: Any) {
            ii(tag, String.format(format, *args))
        }

        fun ii(tag: String, message: String) {
            internalLog(INFO, tag, message)
        }

        fun ii(tag: String, throwable: Throwable, format: String, vararg args: Any) {
            ii(tag, throwable, String.format(format, *args))
        }

        fun ii(tag: String, throwable: Throwable, message: String) {
            internalLog(INFO, tag, throwable, message)
        }

        fun w(format: String, vararg args: Any) {
            w(String.format(format, *args))
        }

        fun w(message: String) {
            internalLog(WARN, TAG, message)
        }

        fun ww(tag: String, format: String, vararg args: Any) {
            ww(tag, String.format(format, *args))
        }

        fun ww(tag: String, message: String) {
            internalLog(WARN, tag, message)
        }

        fun ww(tag: String, throwable: Throwable, format: String, vararg args: Any) {
            ww(tag, throwable, String.format(format, *args))
        }

        fun ww(tag: String, throwable: Throwable, message: String) {
            internalLog(WARN, tag, throwable, message)
        }

        fun e(format: String, vararg args: Any) {
            e(String.format(format, *args))
        }

        fun e(message: String) {
            internalLog(ERROR, TAG, message)
        }

        fun ee(tag: String, format: String, vararg args: Any) {
            ee(tag, String.format(format, *args))
        }

        fun ee(tag: String, message: String) {
            internalLog(ERROR, tag, message)
        }

        fun ee(tag: String, throwable: Throwable, format: String, vararg args: Any) {
            ee(tag, throwable, String.format(format, *args))
        }

        fun ee(tag: String, throwable: Throwable, message: String) {
            internalLog(ERROR, tag, throwable, message)
        }
    }
}
