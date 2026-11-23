package com.kjota.bajalo.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.OutputStream

data class LibraryItem(
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Long,
    /** Segundos desde 1970. */
    val dateAdded: Long,
) {
    val isVideo: Boolean get() = mime.startsWith("video/")
    val isImage: Boolean get() = mime.startsWith("image/")
    val isAudio: Boolean get() = mime.startsWith("audio/")
}

/**
 * Guarda los archivos en la galería del teléfono (Movies/Bajalo, Pictures/Bajalo, Music/Bajalo).
 * No necesita permisos de almacenamiento (Android 10 o superior).
 */
object MediaSaver {
    private const val ROOT = "Bajalo"

    fun mimeOf(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    fun saveFile(ctx: Context, file: File, displayName: String, subDir: String? = null): Uri {
        return saveStream(ctx, displayName, mimeOf(displayName), subDir) { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
    }

    fun saveFromUri(ctx: Context, src: Uri, displayName: String, subDir: String? = null): Uri {
        return saveStream(ctx, displayName, mimeOf(displayName), subDir) { out ->
            val input = ctx.contentResolver.openInputStream(src)
                ?: throw IllegalStateException("No se pudo leer el archivo")
            input.use { it.copyTo(out) }
        }
    }

    private fun collectionFor(mime: String): Pair<Uri, String> = when {
        mime.startsWith("video/") ->
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to Environment.DIRECTORY_MOVIES
        mime.startsWith("audio/") ->
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to Environment.DIRECTORY_MUSIC
        mime.startsWith("image/") ->
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to Environment.DIRECTORY_PICTURES
        else ->
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to Environment.DIRECTORY_DOWNLOADS
    }

    private fun saveStream(
        ctx: Context,
        displayName: String,
        mime: String,
        subDir: String?,
        write: (OutputStream) -> Unit,
    ): Uri {
        val (collection, baseDir) = collectionFor(mime)
        val relative = baseDir + "/" + ROOT + (if (subDir != null) "/$subDir" else "")

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = ctx.contentResolver
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("No se pudo crear el archivo en la galería")

        try {
            val out = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("No se pudo escribir en la galería")
            out.use { write(it) }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (t: Throwable) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            throw t
        }
        return uri
    }

    /** Lista lo que esta app ha guardado en la galería. */
    fun listDownloads(ctx: Context): List<LibraryItem> {
        val files = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("%/$ROOT/%")
        val order = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val result = mutableListOf<LibraryItem>()
        ctx.contentResolver.query(files, projection, selection, args, order)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val iMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val iDate = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                val mime = c.getString(iMime) ?: "application/octet-stream"
                val base = collectionFor(mime).first
                result.add(
                    LibraryItem(
                        uri = ContentUris.withAppendedId(base, c.getLong(iId)),
                        name = c.getString(iName) ?: "archivo",
                        mime = mime,
                        size = c.getLong(iSize),
                        dateAdded = c.getLong(iDate),
                    )
                )
            }
        }
        return result
    }

    fun delete(ctx: Context, item: LibraryItem): Boolean {
        return try {
            ctx.contentResolver.delete(item.uri, null, null) > 0
        } catch (_: Exception) {
            false
        }
    }
}
