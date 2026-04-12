package com.nexpos.laundry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexPosLaundryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val msg = buildString {
                    append("CRASH: ${throwable.javaClass.simpleName}\n\n")
                    append(throwable.message ?: "(no message)").append("\n\n")
                    append("--- Stack trace ---\n")
                    append(throwable.stackTraceToString().take(1500))
                }
                val prefs = getSharedPreferences("crash_log", MODE_PRIVATE)
                prefs.edit().putString("last_crash", msg).apply()
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
