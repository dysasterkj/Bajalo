package com.kjota.bajalo.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kjota.bajalo.engine.Site

/**
 * Pantalla para iniciar sesión dentro de la app. La sesión queda guardada solo en el teléfono
 * y se usa para que yt-dlp pueda ver historias y contenido que exige cuenta.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(site: Site, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val ctx = LocalContext.current

    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var jsError by remember { mutableStateOf<String?>(null) }
    var showDiag by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
            }
            Text(
                text = "Iniciar sesión en ${site.label}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onDone) { Text("Listo") }
        }
        Text(
            text = "Entra con tu cuenta y, cuando veas tu inicio o tu perfil, toca «Listo».",
            style = MaterialTheme.typography.bodySmall,
            color = Glass.Soft,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (progress in 1..99 && error == null) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            androidx.compose.material3.TextButton(onClick = { error = null; web?.reload() }) { Text("Recargar") }
            androidx.compose.material3.TextButton(onClick = { showDiag = !showDiag }) { Text("Diagnóstico") }
        }
        if (showDiag) {
            Text(
                text = "Progreso $progress%. Último error de la página: ${jsError ?: "ninguno"}",
                style = MaterialTheme.typography.bodySmall,
                color = Glass.Soft,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(android.graphics.Color.WHITE)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.loadWithOverviewMode = true
                        settings.databaseEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.mediaPlaybackRequiresUserGesture = true
                        // La app es oscura, y Android intentaba oscurecer también la página
                        // del sitio, dejándola negra. Se desactiva.
                        if (Build.VERSION.SDK_INT >= 33) {
                            settings.isAlgorithmicDarkeningAllowed = false
                        } else if (Build.VERSION.SDK_INT >= 29) {
                            @Suppress("DEPRECATION")
                            settings.forceDark = WebSettings.FORCE_DARK_OFF
                        }
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        settings.useWideViewPort = true
                        // Mismo navegador del teléfono, pero sin la marca de "WebView" que
                        // hace que Instagram y Facebook se nieguen a cargar.
                        settings.userAgentString = settings.userAgentString
                            .replace("; wv", "")
                            .replace(Regex("Version/\\d+\\.\\d+ "), "")
                        val cookies = CookieManager.getInstance()
                        cookies.setAcceptCookie(true)
                        cookies.setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                                if (m?.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    jsError = m.message().take(160)
                                }
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val scheme = request?.url?.scheme?.lowercase()
                                // Instagram/Facebook intentan abrir su app (intent://, fb://…):
                                // se bloquea para que la página siga cargando aquí.
                                return scheme != "http" && scheme != "https"
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?,
                            ): Boolean {
                                error = "El navegador interno se detuvo."
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                error = null
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                err: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    error = "No se pudo cargar la página (${err?.description ?: "error de red"})."
                                }
                            }
                        }
                        web = this
                        loadUrl(site.loginUrl)
                    }
                },
            )

            error?.let { msg ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { error = null; web?.loadUrl(site.loginUrl) }) { Text("Reintentar") }
                    OutlinedButton(onClick = {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(site.loginUrl))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("Abrir en el navegador") }
                }
            }
        }
    }
}
