package com.kjota.bajalo.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kjota.bajalo.engine.Site

private const val MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/126.0.0.0 Mobile Safari/537.36"

/**
 * Pantalla para iniciar sesión dentro de la app. La sesión queda guardada solo en el teléfono
 * y se usa para que yt-dlp pueda ver historias y contenido que exige cuenta.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(site: Site, onDone: () -> Unit) {
    BackHandler(onBack = onDone)

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = MOBILE_UA
                    val cookies = CookieManager.getInstance()
                    cookies.setAcceptCookie(true)
                    cookies.setAcceptThirdPartyCookies(this, true)
                    webViewClient = WebViewClient()
                    loadUrl(site.loginUrl)
                }
            },
        )
    }
}
