package com.runtracker.shared.util

/** Multiplatform logger — delegates to android.util.Log on Android, NSLog on iOS. */
expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
}
