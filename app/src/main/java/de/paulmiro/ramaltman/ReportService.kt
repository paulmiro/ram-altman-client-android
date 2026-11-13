package de.paulmiro.ramaltman

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

/** Foreground service that sends a report every configured interval while it runs. */
class ReportService : Service() {
    private var thread: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Reporting", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            1,
            Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle("RAM Altman")
                .setContentText("Reporting memory usage")
                .setOngoing(true)
                .build(),
        )
        // Without this the CPU sleeps with the screen off and the loop stalls.
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ramaltman:report")
                .apply { acquire() }
        }

        // Every start (re)starts the loop so changed settings apply immediately.
        thread?.interrupt()
        thread = Thread {
            val settings = Settings(applicationContext)
            try {
                while (true) {
                    sendReport(applicationContext)
                    Thread.sleep(settings.intervalMinutes * 60_000L)
                }
            } catch (_: InterruptedException) {
            }
        }.also { it.start() }
        return START_STICKY
    }

    override fun onDestroy() {
        thread?.interrupt()
        wakeLock?.release()
    }

    private companion object {
        const val CHANNEL = "report"
    }
}

/** Sends one memory report: PUT /api/v1/report/{username}/{hostname}. Blocking. */
fun sendReport(context: Context) {
    val settings = Settings(context)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

    val memory = ActivityManager.MemoryInfo()
    context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)

    val body = JSONObject()
        .put("free_ram_bytes", memory.availMem)
        .put("total_ram_bytes", memory.totalMem)
        .put("timeout_minutes", settings.timeoutMinutes)
    if (settings.color.isNotBlank()) body.put("color", settings.color.trim())

    val url = settings.serverUrl.trim().trimEnd('/') +
        "/api/v1/report/${Uri.encode(settings.username.trim())}/${Uri.encode(settings.hostname.trim())}"

    // Failures are only shown in the status; the next interval simply tries again.
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${settings.token.trim()}")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = connection.responseCode
            settings.status = if (code in 200..299) {
                "$time: OK"
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?.let { runCatching { JSONObject(it).getString("error") }.getOrDefault(it) }
                "$time: HTTP $code ${error.orEmpty()}"
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        settings.status = "$time: ${e.message ?: e.javaClass.simpleName}"
    }
}
