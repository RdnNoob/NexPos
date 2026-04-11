package com.nexpos.admin

import android.app.Application
import android.app.AlertDialog
import android.content.Intent
import android.os.Looper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexPosAdminApp : Application() {

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

    companion object {
        fun getLastCrash(app: Application): String? {
            val prefs = app.getSharedPreferences("crash_log", MODE_PRIVATE)
            val msg = prefs.getString("last_crash", null)
            prefs.edit().remove("last_crash").apply()
            return msg
        }
    }
}
