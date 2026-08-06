package com.demushrenich.archim.debug

import android.content.Context
import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogcatFileWriter {

    private const val TAG = "LogcatFileWriter"
    private const val MAX_SESSIONS_TO_KEEP = 15

    @Volatile
    private var isRunning = false

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        val logsDir = File(context.filesDir, "logs").apply { mkdirs() }
        cleanupOldSessions(logsDir)

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val logFile = File(logsDir, "session_$timestamp.txt")

        Thread {
            try {
                Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor()

                val pid = Process.myPid()
                val process = Runtime.getRuntime().exec(
                    arrayOf("logcat", "-v", "threadtime", "--pid=$pid")
                )

                logFile.bufferedWriter().use { writer ->
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            writer.write(line ?: "")
                            writer.newLine()
                            writer.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logcat capture failed", e)
            }
        }.apply {
            isDaemon = true
            name = "LogcatFileWriter"
        }.start()

        Log.d(TAG, "Logging session started: ${logFile.absolutePath}")
        LifecycleMarkers.attach(context)
    }

    private fun cleanupOldSessions(logsDir: File) {
        val files = logsDir.listFiles { f -> f.isFile && f.name.startsWith("session_") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        files.drop(MAX_SESSIONS_TO_KEEP).forEach { it.delete() }
    }

    fun logsDirectory(context: Context): File =
        File(context.filesDir, "logs")
}