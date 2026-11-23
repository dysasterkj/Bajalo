package com.kjota.bajalo.download

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kjota.bajalo.BajaloApp
import com.kjota.bajalo.MainActivity
import com.kjota.bajalo.R
import com.kjota.bajalo.data.MediaSaver
import com.kjota.bajalo.engine.Accounts
import com.kjota.bajalo.engine.Engine
import com.kjota.bajalo.engine.Prober
import com.kjota.bajalo.util.Errors
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Servicio en primer plano: mantiene las descargas vivas aunque salgas de la app
 * (por ejemplo, para volver a Instagram a copiar otro enlace).
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(2)
    private val active = AtomicInteger(0)
    private val lastStartId = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId.set(startId)
        ServiceCompat.startForeground(
            this,
            FG_ID,
            progressNotification("Preparando descarga…", 0, true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (taskId == null) {
            stopIfIdle()
            return START_NOT_STICKY
        }

        active.incrementAndGet()
        scope.launch {
            try {
                gate.withPermit { runTask(taskId) }
            } finally {
                active.decrementAndGet()
                stopIfIdle()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun stopIfIdle() {
        if (active.get() == 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(lastStartId.get())
        }
    }

    // ---------------------------------------------------------------- descarga

    private suspend fun runTask(id: String) {
        val task = DownloadCenter.get(id) ?: return
        if (task.status == TaskStatus.CANCELED) return

        DownloadCenter.update(id) { it.copy(status = TaskStatus.RUNNING, message = "Preparando…", progress = 0f) }
        refreshForeground()

        val temp = File(cacheDir, "dl/$id")
        temp.deleteRecursively()
        temp.mkdirs()

        try {
            Engine.awaitReady()
            val request = buildRequest(task, temp)

            var lastPct = -1
            val onProgress: (Float, Long, String) -> Unit = { p, _, _ ->
                val pct = p.toInt()
                if (pct != lastPct) {
                    lastPct = pct
                    DownloadCenter.update(id) {
                        it.copy(progress = (p / 100f).coerceIn(0f, 1f), message = "Descargando… $pct%")
                    }
                    refreshForeground()
                }
            }

            YoutubeDL.getInstance().execute(request, id, onProgress)

            DownloadCenter.update(id) { it.copy(progress = 1f, message = "Guardando en la galería…") }

            val files = temp.walkTopDown().filter { it.isFile && isFinalFile(it) }.toList()
            if (files.isEmpty()) throw IllegalStateException("No se descargó ningún archivo")

            val saved = files.map { MediaSaver.saveFile(this, it, cleanName(it.name)).toString() }

            DownloadCenter.update(id) {
                it.copy(status = TaskStatus.DONE, progress = 1f, message = "Guardado en tu galería", saved = saved)
            }
            notifyFinished(task, true)
        } catch (e: YoutubeDL.CanceledException) {
            DownloadCenter.update(id) { it.copy(status = TaskStatus.CANCELED, message = "Cancelada") }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            DownloadCenter.update(id) { it.copy(status = TaskStatus.FAILED, message = Errors.friendly(t)) }
            notifyFinished(task, false)
        } finally {
            temp.deleteRecursively()
            refreshForeground()
        }
    }

    private fun buildRequest(task: DownloadTask, dir: File): YoutubeDLRequest {
        val r = YoutubeDLRequest(task.url)
        r.addOption("-o", dir.absolutePath + "/%(title).70B [%(id).24B].%(ext)s")
        r.addOption("--newline")
        r.addOption("--no-mtime")
        r.addOption("--no-warnings")
        r.addOption("--retries", 5)
        r.addOption("--fragment-retries", 5)
        r.addOption("--concurrent-fragments", 4)
        r.addOption("--socket-timeout", 30)
        r.addOption("--playlist-end", 30)
        if (Prober.looksLikeSingleYoutubeVideo(task.url)) r.addOption("--no-playlist")

        val cookies = Accounts.cookieFileOrNull(this)
        if (cookies != null) {
            // Copia por descarga: yt-dlp reescribe este archivo al terminar.
            val copy = File(dir, "cookies.txt")
            cookies.copyTo(copy, overwrite = true)
            r.addOption("--cookies", copy.absolutePath)
        }

        if (task.audioOnly) {
            r.addOption("-x")
            r.addOption("--audio-format", "mp3")
            r.addOption("--audio-quality", 0)
        } else {
            r.addOption("-f", "bv*+ba/b")
            val sort = if (task.height > 0) {
                "res:${task.height},vcodec:h264,acodec:aac"
            } else {
                "res,vcodec:h264,acodec:aac"
            }
            r.addOption("-S", sort)
            r.addOption("--merge-output-format", "mp4")
        }
        return r
    }

    private fun isFinalFile(f: File): Boolean {
        val ext = f.extension.lowercase()
        return ext !in setOf("part", "ytdl", "temp", "tmp", "json", "txt", "vtt", "srt")
    }

    /** Quita el " [id]" que se agrega al final del nombre para evitar choques entre archivos. */
    private fun cleanName(name: String): String =
        name.replace(Regex("\\s\\[[^\\]]{1,24}\\](?=\\.[A-Za-z0-9]{2,5}$)"), "")

    // ----------------------------------------------------------- notificaciones

    private fun openAppIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun progressNotification(text: String, pct: Int, indeterminate: Boolean): Notification =
        NotificationCompat.Builder(this, BajaloApp.CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Bajalo")
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, pct, indeterminate)
            .setContentIntent(openAppIntent())
            .build()

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun refreshForeground() {
        if (!canNotify()) return
        val running = DownloadCenter.tasks.value.filter { it.status == TaskStatus.RUNNING }
        if (running.isEmpty()) return
        val text = if (running.size == 1) {
            "Descargando: ${running[0].title}"
        } else {
            "Descargando ${running.size} archivos"
        }
        val pct = (running.map { it.progress }.average() * 100).toInt().coerceIn(0, 100)
        NotificationManagerCompat.from(this).notify(FG_ID, progressNotification(text, pct, pct == 0))
    }

    @SuppressLint("MissingPermission")
    private fun notifyFinished(task: DownloadTask, ok: Boolean) {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(this, BajaloApp.CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(if (ok) "Descarga lista" else "No se pudo descargar")
            .setContentText(task.title)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        NotificationManagerCompat.from(this).notify(task.id.hashCode(), n)
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        private const val FG_ID = 1001

        fun start(ctx: Context, taskId: String) {
            val i = Intent(ctx, DownloadService::class.java).putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(ctx, i)
        }

        /** Llamar fuera del hilo principal. */
        fun cancel(taskId: String) {
            DownloadCenter.update(taskId) {
                if (it.isActive) it.copy(status = TaskStatus.CANCELED, message = "Cancelada") else it
            }
            try {
                YoutubeDL.getInstance().destroyProcessById(taskId)
            } catch (_: Throwable) {
            }
        }
    }
}
