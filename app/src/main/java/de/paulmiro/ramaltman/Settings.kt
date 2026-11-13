package de.paulmiro.ramaltman

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit

const val MIN_INTERVAL_MINUTES = 1
const val DEFAULT_TIMEOUT_MINUTES = 5
const val MIN_TIMEOUT_MINUTES = 1 // Limits of the API's timeout_minutes.
const val MAX_TIMEOUT_MINUTES = 60

class Settings(private val context: Context) {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) = prefs.edit { putBoolean("enabled", v) }

    var serverUrl: String
        get() = prefs.getString("serverUrl", "https://y7v.de")!!
        set(v) = prefs.edit { putString("serverUrl", v) }

    var token: String
        get() = prefs.getString("token", "")!!
        set(v) = prefs.edit { putString("token", v) }

    var username: String
        get() = prefs.getString("username", "")!!
        set(v) = prefs.edit { putString("username", v) }

    var hostname: String
        get() = prefs.getString("hostname", Build.MODEL.replace(Regex("[^A-Za-z0-9._-]"), "-"))!!
        set(v) = prefs.edit { putString("hostname", v) }

    var color: String
        get() = prefs.getString("color", "")!!
        set(v) = prefs.edit { putString("color", v) }

    var intervalMinutes: Int
        get() = prefs.getInt("intervalMinutes", 1)
        set(v) = prefs.edit { putInt("intervalMinutes", v) }

    var timeoutMinutes: Int
        get() = prefs.getInt("timeoutMinutes", DEFAULT_TIMEOUT_MINUTES)
        set(v) = prefs.edit { putInt("timeoutMinutes", v) }

    var status: String
        get() = prefs.getString("status", "")!!
        set(v) = prefs.edit { putString("status", v) }

    /** Starts, restarts or stops the reporting service according to the current settings. */
    fun schedule() {
        val intent = Intent(context, ReportService::class.java)
        if (enabled) context.startForegroundService(intent) else context.stopService(intent)
    }

    /** Sends one report right now, independent of the on/off switch. */
    fun reportNow() {
        Thread { sendReport(context) }.start()
    }
}
