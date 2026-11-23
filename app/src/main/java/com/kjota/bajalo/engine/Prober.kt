package com.kjota.bajalo.engine

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File

data class MediaEntry(
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val durationSec: Int,
    val resolutions: List<Int>,
    val isImage: Boolean,
)

data class ProbeResult(
    val sourceUrl: String,
    val entries: List<MediaEntry>,
) {
    /** Resoluciones disponibles (lado menor del video), de mayor a menor. */
    val resolutions: List<Int>
        get() = entries.flatMap { it.resolutions }.distinct().sortedDescending()

    val allImages: Boolean
        get() = entries.isNotEmpty() && entries.all { it.isImage }
}

/** Analiza un enlace (sin descargar) para mostrar vista previa y calidades. */
object Prober {

    fun looksLikeSingleYoutubeVideo(url: String): Boolean =
        url.contains("youtu.be/") || (url.contains("youtube.com") && url.contains("v="))

    fun probe(ctx: Context, url: String): ProbeResult {
        val req = YoutubeDLRequest(url)
        req.addOption("--dump-json")
        req.addOption("--no-warnings")
        req.addOption("--ignore-errors")
        req.addOption("--socket-timeout", 20)
        req.addOption("--playlist-end", 30)
        if (looksLikeSingleYoutubeVideo(url)) req.addOption("--no-playlist")

        val cookies = Accounts.cookieFileOrNull(ctx)
        if (cookies != null) {
            // Copia propia: yt-dlp reescribe el archivo de cookies al terminar.
            val copy = File(ctx.cacheDir, "probe_cookies.txt")
            cookies.copyTo(copy, overwrite = true)
            req.addOption("--cookies", copy.absolutePath)
        }

        val resp = YoutubeDL.getInstance().execute(req, null, null)

        val entries = resp.out
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("{") }
            .mapNotNull { parseEntry(it) }
            .toList()

        if (entries.isEmpty()) {
            val detail = resp.err.trim()
            throw IllegalStateException(
                if (detail.isNotEmpty()) detail else "No encontré ningún video en ese enlace"
            )
        }
        return ProbeResult(sourceUrl = url, entries = entries)
    }

    private fun str(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        return o.optString(key).takeIf { it.isNotBlank() }
    }

    private fun resOf(w: Int, h: Int): Int = when {
        w > 0 && h > 0 -> minOf(w, h)
        else -> maxOf(w, h)
    }

    private fun parseEntry(json: String): MediaEntry? {
        return try {
            val o = JSONObject(json)
            val title = str(o, "title") ?: str(o, "id") ?: "Video"
            val uploader = str(o, "uploader") ?: str(o, "channel") ?: str(o, "uploader_id")
            val thumb = str(o, "thumbnail")
            val d = o.optDouble("duration", 0.0)
            val duration = if (d.isNaN() || d < 0) 0 else d.toInt()

            val res = linkedSetOf<Int>()
            val formats = o.optJSONArray("formats")
            if (formats != null) {
                for (i in 0 until formats.length()) {
                    val f = formats.optJSONObject(i) ?: continue
                    if (str(f, "vcodec") == "none") continue
                    val r = resOf(f.optInt("width", 0), f.optInt("height", 0))
                    if (r > 0) res.add(r)
                }
            }
            if (res.isEmpty()) {
                val r = resOf(o.optInt("width", 0), o.optInt("height", 0))
                if (r > 0) res.add(r)
            }

            val ext = (str(o, "ext") ?: "").lowercase()
            val isImage = ext in setOf("jpg", "jpeg", "png", "webp")

            MediaEntry(
                title = title,
                uploader = uploader,
                thumbnail = thumb,
                durationSec = duration,
                resolutions = res.toList(),
                isImage = isImage,
            )
        } catch (_: Exception) {
            null
        }
    }
}
