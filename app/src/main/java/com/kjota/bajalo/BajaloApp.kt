package com.kjota.bajalo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.kjota.bajalo.engine.Engine

class BajaloApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        createChannels()
        Engine.start(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Descargas en curso", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Descargas terminadas", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Cargador de miniaturas: entiende imágenes y también cuadros de video. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    companion object {
        const val CHANNEL_PROGRESS = "downloads_progress"
        const val CHANNEL_DONE = "downloads_done"
    }
}
