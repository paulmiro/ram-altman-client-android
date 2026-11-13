package de.paulmiro.ramaltman

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

/** Sends one memory report: PUT /api/v1/report/{username}/{hostname} */
class ReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = Settings(applicationContext)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

        val memory = ActivityManager.MemoryInfo()
        applicationContext.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)

        val body = JSONObject()
            .put("free_ram_bytes", memory.availMem)
            .put("total_ram_bytes", memory.totalMem)
            .put("timeout_minutes", settings.timeoutMinutes)
        if (settings.color.isNotBlank()) body.put("color", settings.color.trim())

        val url = settings.serverUrl.trim().trimEnd('/') +
            "/api/v1/report/${Uri.encode(settings.username.trim())}/${Uri.encode(settings.hostname.trim())}"

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
        // Failures are only shown in the status; the next periodic run simply tries again.
        Result.success()
    }
}
