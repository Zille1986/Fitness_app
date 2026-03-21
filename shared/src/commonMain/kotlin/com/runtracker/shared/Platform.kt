package com.runtracker.shared

expect fun platformName(): String

object Platform {
    val name: String get() = platformName()
    val isAndroid: Boolean get() = name == "Android"
    val isIOS: Boolean get() = name == "iOS"
}
