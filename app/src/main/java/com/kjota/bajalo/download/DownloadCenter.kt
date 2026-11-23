package com.kjota.bajalo.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TaskStatus { QUEUED, RUNNING, DONE, FAILED, CANCELED }

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    /** 0 = mejor calidad disponible; si no, límite de resolución (lado menor). */
    val height: Int,
    val audioOnly: Boolean,
    val status: TaskStatus = TaskStatus.QUEUED,
    val progress: Float = 0f,
    val message: String = "En cola…",
    /** Direcciones (content://) de los archivos guardados en la galería. */
    val saved: List<String> = emptyList(),
) {
    val isActive: Boolean get() = status == TaskStatus.QUEUED || status == TaskStatus.RUNNING
}

/** Lista compartida de descargas: la escribe el servicio y la lee la pantalla. */
object DownloadCenter {
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun add(task: DownloadTask) {
        _tasks.update { listOf(task) + it }
    }

    fun get(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    fun update(id: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) block(it) else it } }
    }

    fun remove(id: String) {
        _tasks.update { list -> list.filterNot { it.id == id } }
    }

    fun clearFinished() {
        _tasks.update { list -> list.filter { it.isActive } }
    }
}
