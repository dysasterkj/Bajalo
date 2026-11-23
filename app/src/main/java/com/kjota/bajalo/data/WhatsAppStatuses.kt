package com.kjota.bajalo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

enum class WaApp(
    val label: String,
    val pkg: String,
    val dirName: String,
    val prefKey: String,
) {
    WHATSAPP("WhatsApp", "com.whatsapp", "WhatsApp", "wa_tree_uri"),
    BUSINESS("WhatsApp Business", "com.whatsapp.w4b", "WhatsApp Business", "wa_tree_uri_business"),
}

data class StatusFile(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val modified: Long,
    val size: Long,
)

/**
 * Lee los estados de WhatsApp que el propio teléfono ya guardó al verlos
 * (carpeta .Statuses) y permite copiarlos a la galería.
 *
 * Android no deja leer esa carpeta sin permiso, así que el usuario la concede una sola vez
 * con el selector de carpetas del sistema.
 */
object WhatsAppStatuses {
    private const val SAVED_KEY = "saved_status_names"

    /** Carpeta donde se abre el selector para que el usuario solo toque "Usar esta carpeta". */
    fun initialPickerUri(app: WaApp): Uri {
        val docId = if (Build.VERSION.SDK_INT >= 30) {
            "primary:Android/media/${app.pkg}/${app.dirName}/Media/.Statuses"
        } else {
            "primary:${app.dirName}/Media/.Statuses"
        }
        return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docId)
    }

    /** Devuelve la carpeta concedida si el permiso sigue vigente. */
    fun linkedUri(ctx: Context, app: WaApp): Uri? {
        val stored = Prefs.getString(ctx, app.prefKey) ?: return null
        val uri = Uri.parse(stored)
        val ok = ctx.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        return if (ok) uri else null
    }

    /** Guarda el permiso. Devuelve false si la carpeta elegida no contiene estados. */
    fun link(ctx: Context, app: WaApp, uri: Uri): Boolean {
        try {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            // Algunos teléfonos solo entregan permiso temporal; se intenta igual.
        }
        val root = DocumentFile.fromTreeUri(ctx, uri) ?: return false
        if (resolveDir(root) == null) return false
        Prefs.putString(ctx, app.prefKey, uri.toString())
        return true
    }

    fun unlink(ctx: Context, app: WaApp) {
        val stored = Prefs.getString(ctx, app.prefKey)
        if (stored != null) {
            try {
                ctx.contentResolver.releasePersistableUriPermission(
                    Uri.parse(stored),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
            }
        }
        Prefs.putString(ctx, app.prefKey, null)
    }

    private fun resolveDir(root: DocumentFile): DocumentFile? {
        if (root.name == ".Statuses") return root
        root.findFile(".Statuses")?.let { return it }
        val media = if (root.name == "Media") root else root.findFile("Media")
        media?.findFile(".Statuses")?.let { return it }
        for (dir in listOf("WhatsApp", "WhatsApp Business")) {
            root.findFile(dir)?.findFile("Media")?.findFile(".Statuses")?.let { return it }
        }
        return null
    }

    fun list(ctx: Context, treeUri: Uri): List<StatusFile> {
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return emptyList()
        val dir = resolveDir(root) ?: return emptyList()
        val videoExt = listOf(".mp4", ".3gp", ".mkv", ".webm")
        val imageExt = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")

        val result = mutableListOf<StatusFile>()
        for (f in dir.listFiles()) {
            val name = f.name ?: continue
            if (!f.isFile) continue
            val lower = name.lowercase()
            val isVideo = videoExt.any { lower.endsWith(it) }
            val isImage = imageExt.any { lower.endsWith(it) }
            if (!isVideo && !isImage) continue
            result.add(StatusFile(f.uri, name, isVideo, f.lastModified(), f.length()))
        }
        return result.sortedByDescending { it.modified }
    }

    fun savedNames(ctx: Context): Set<String> = Prefs.getStringSet(ctx, SAVED_KEY)

    fun markSaved(ctx: Context, names: Collection<String>) {
        Prefs.putStringSet(ctx, SAVED_KEY, savedNames(ctx) + names)
    }

    /** Copia un estado a la galería (Movies/Bajalo/WhatsApp o Pictures/Bajalo/WhatsApp). */
    fun saveToGallery(ctx: Context, file: StatusFile): Uri =
        MediaSaver.saveFromUri(ctx, file.uri, file.name, "WhatsApp")
}
