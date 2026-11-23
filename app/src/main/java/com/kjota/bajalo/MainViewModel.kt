package com.kjota.bajalo

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kjota.bajalo.data.LibraryItem
import com.kjota.bajalo.data.MediaSaver
import com.kjota.bajalo.data.StatusFile
import com.kjota.bajalo.data.WaApp
import com.kjota.bajalo.data.WhatsAppStatuses
import com.kjota.bajalo.download.DownloadCenter
import com.kjota.bajalo.download.DownloadService
import com.kjota.bajalo.download.DownloadTask
import com.kjota.bajalo.download.TaskStatus
import com.kjota.bajalo.engine.Accounts
import com.kjota.bajalo.engine.Engine
import com.kjota.bajalo.engine.ProbeResult
import com.kjota.bajalo.engine.Prober
import com.kjota.bajalo.engine.Site
import com.kjota.bajalo.util.Errors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Opción de calidad que se muestra como botón al descargar. */
data class Quality(val label: String, val height: Int, val audioOnly: Boolean)

sealed interface HomeState {
    data object Idle : HomeState
    data object Loading : HomeState
    data class Ready(val result: ProbeResult, val options: List<Quality>) : HomeState
    data class Error(val message: String) : HomeState
}

object Tabs {
    const val DOWNLOAD = 0
    const val STATUS = 1
    const val LIBRARY = 2
    const val SETTINGS = 3
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Application get() = getApplication()

    // ------------------------------------------------------------ navegación
    var tab by mutableIntStateOf(Tabs.DOWNLOAD)
    var loginSite by mutableStateOf<Site?>(null)
        private set

    // ---------------------------------------------------------------- mensajes
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    fun toast(text: String) {
        _messages.tryEmit(text)
    }

    // ------------------------------------------------------------------ inicio
    var urlInput by mutableStateOf("")
    var home by mutableStateOf<HomeState>(HomeState.Idle)
        private set
    var quality by mutableStateOf(Quality("Mejor calidad", 0, false))

    fun onSharedText(text: String) {
        val url = extractUrl(text) ?: return
        tab = Tabs.DOWNLOAD
        urlInput = url
        analyze()
    }

    fun analyze() {
        val url = extractUrl(urlInput)
        if (url == null) {
            home = HomeState.Error("Pega un enlace que empiece con http:// o https://")
            return
        }
        urlInput = url
        home = HomeState.Loading
        Accounts.refreshCookieFile(ctx) // hilo principal (CookieManager)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Engine.awaitReady()
                    Prober.probe(ctx, url)
                }
                val options = qualitiesFor(result)
                quality = options.first()
                home = HomeState.Ready(result, options)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                home = HomeState.Error(Errors.friendly(t))
            }
        }
    }

    fun clearHome() {
        home = HomeState.Idle
    }

    fun startDownload() {
        val ready = home as? HomeState.Ready ?: return
        val r = ready.result
        val first = r.entries.first()
        val q = quality
        val title = if (r.entries.size == 1) first.title else "${r.entries.size} archivos"

        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            url = r.sourceUrl,
            title = title,
            thumbnail = first.thumbnail,
            height = q.height,
            audioOnly = q.audioOnly,
        )
        Accounts.refreshCookieFile(ctx)
        DownloadCenter.add(task)
        DownloadService.start(ctx, task.id)

        home = HomeState.Idle
        urlInput = ""
        toast("Descarga iniciada")
    }

    fun cancelTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) { DownloadService.cancel(id) }
    }

    fun retryTask(id: String) {
        val t = DownloadCenter.get(id) ?: return
        if (t.isActive) return
        Accounts.refreshCookieFile(ctx)
        DownloadCenter.update(id) {
            it.copy(status = TaskStatus.QUEUED, progress = 0f, message = "En cola…", saved = emptyList())
        }
        DownloadService.start(ctx, id)
    }

    fun removeTask(id: String) {
        DownloadCenter.remove(id)
    }

    fun clearFinishedTasks() {
        DownloadCenter.clearFinished()
    }

    // ----------------------------------------------------- estados de WhatsApp
    var waApp by mutableStateOf(WaApp.WHATSAPP)
        private set
    var waLinked by mutableStateOf(false)
        private set
    var waLoading by mutableStateOf(false)
        private set
    var waFiles by mutableStateOf<List<StatusFile>>(emptyList())
        private set
    var waSelected by mutableStateOf<Set<Uri>>(emptySet())
        private set
    var waSaved by mutableStateOf<Set<String>>(emptySet())
        private set

    fun switchWa(app: WaApp) {
        if (app == waApp) return
        waApp = app
        waSelected = emptySet()
        loadStatuses()
    }

    fun loadStatuses() {
        val treeUri = WhatsAppStatuses.linkedUri(ctx, waApp)
        waLinked = treeUri != null
        waSaved = WhatsAppStatuses.savedNames(ctx)
        if (treeUri == null) {
            waFiles = emptyList()
            return
        }
        waLoading = true
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                try {
                    WhatsAppStatuses.list(ctx, treeUri)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            waFiles = files
            waSelected = waSelected.filter { sel -> files.any { it.uri == sel } }.toSet()
            waLoading = false
        }
    }

    fun onWaFolderPicked(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { WhatsAppStatuses.link(ctx, waApp, uri) }
            if (ok) {
                toast("Carpeta conectada")
                loadStatuses()
            } else {
                toast("Esa no es la carpeta de estados. Elige la carpeta «.Statuses» de WhatsApp.")
            }
        }
    }

    fun unlinkWa() {
        WhatsAppStatuses.unlink(ctx, waApp)
        waSelected = emptySet()
        loadStatuses()
    }

    fun toggleSelect(uri: Uri) {
        waSelected = if (uri in waSelected) waSelected - uri else waSelected + uri
    }

    fun selectAllStatuses() {
        waSelected = if (waSelected.size == waFiles.size) emptySet() else waFiles.map { it.uri }.toSet()
    }

    fun saveSelectedStatuses() {
        saveStatuses(waFiles.filter { it.uri in waSelected })
    }

    fun saveStatuses(chosen: List<StatusFile>) {
        if (chosen.isEmpty()) return
        viewModelScope.launch {
            val savedNames = withContext(Dispatchers.IO) {
                val names = mutableListOf<String>()
                for (f in chosen) {
                    try {
                        WhatsAppStatuses.saveToGallery(ctx, f)
                        names.add(f.name)
                    } catch (_: Exception) {
                    }
                }
                WhatsAppStatuses.markSaved(ctx, names)
                names
            }
            waSaved = WhatsAppStatuses.savedNames(ctx)
            waSelected = emptySet()
            if (savedNames.size == chosen.size) {
                toast(if (chosen.size == 1) "Estado guardado en tu galería" else "${chosen.size} estados guardados en tu galería")
            } else {
                toast("Se guardaron ${savedNames.size} de ${chosen.size}")
            }
        }
    }

    // --------------------------------------------------------------- descargas
    var library by mutableStateOf<List<LibraryItem>>(emptyList())
        private set

    fun refreshLibrary() {
        viewModelScope.launch {
            library = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.listDownloads(ctx)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    fun deleteItem(item: LibraryItem) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { MediaSaver.delete(ctx, item) }
            toast(if (ok) "Eliminado" else "No se pudo eliminar")
            refreshLibrary()
        }
    }

    // ----------------------------------------------------------------- ajustes
    var loggedIn by mutableStateOf<Set<Site>>(emptySet())
        private set
    var updatingEngine by mutableStateOf(false)
        private set

    fun refreshAccounts() {
        loggedIn = Accounts.loggedInSites()
    }

    fun openLogin(site: Site) {
        loginSite = site
    }

    fun closeLogin() {
        Accounts.flush()
        loginSite = null
        refreshAccounts()
    }

    fun logout(site: Site) {
        Accounts.logout(site)
        refreshAccounts()
        toast("Sesión de ${site.label} cerrada")
    }

    fun updateEngine() {
        if (updatingEngine) return
        updatingEngine = true
        viewModelScope.launch {
            try {
                toast(Engine.update(ctx))
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                toast("No se pudo actualizar: revisa tu conexión")
            } finally {
                updatingEngine = false
            }
        }
    }

    // ----------------------------------------------------------------- helpers
    companion object {
        fun extractUrl(text: String): String? {
            val m = Regex("https?://\\S+").find(text) ?: return null
            return m.value.trimEnd('.', ',', ')', ']', '!', '?', '"', '\'')
        }

        fun qualitiesFor(r: ProbeResult): List<Quality> {
            if (r.allImages) return listOf(Quality("Original", 0, false))
            val res = r.resolutions.filter { it >= 144 }
            val best = if (res.isEmpty()) {
                Quality("Mejor calidad", 0, false)
            } else {
                Quality("Mejor · ${resLabel(res.first())}", 0, false)
            }
            val list = mutableListOf(best)
            res.drop(1).take(5).forEach { list.add(Quality(resLabel(it), it, false)) }
            list.add(Quality("Solo audio (MP3)", 0, true))
            return list
        }

        private fun resLabel(h: Int): String = when {
            h >= 2160 -> "4K"
            h >= 1440 -> "2K"
            else -> "${h}p"
        }
    }
}
