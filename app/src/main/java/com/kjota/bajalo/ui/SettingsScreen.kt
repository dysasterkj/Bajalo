@file:OptIn(ExperimentalMaterial3Api::class)

package com.kjota.bajalo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kjota.bajalo.MainViewModel
import com.kjota.bajalo.Tabs
import com.kjota.bajalo.engine.Engine
import com.kjota.bajalo.engine.Site

@Composable
fun SettingsScreen(vm: MainViewModel) {
    LaunchedEffect(vm.tab) {
        if (vm.tab == Tabs.SETTINGS) vm.refreshAccounts()
    }
    val version by Engine.version.collectAsStateWithLifecycle()
    val soft = Glass.Soft
    val small = MaterialTheme.typography.bodySmall

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            InfoCard(title = "Cuentas") {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Inicia sesión si quieres bajar historias o contenido que Instagram, Facebook o X " +
                            "solo muestran a quien tiene cuenta. Tu sesión se queda solo en este teléfono.",
                        style = small, color = soft,
                    )
                    Site.values().forEach { site ->
                        val logged = site in vm.loggedIn
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(site.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (logged) "Sesión iniciada" else "Sin sesión",
                                    style = small,
                                    color = if (logged) MaterialTheme.colorScheme.primary else soft,
                                )
                            }
                            if (logged) {
                                OutlinedButton(onClick = { vm.logout(site) }) { Text("Cerrar sesión") }
                            } else {
                                Button(onClick = { vm.openLogin(site) }) { Text("Iniciar sesión") }
                            }
                        }
                    }
                }
            }
        }

        item {
            InfoCard(title = "Motor de descargas") {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("yt-dlp ${version ?: "…"}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Las redes sociales cambian seguido. Bajalo se actualiza solo una vez al día; " +
                            "si algo deja de funcionar, actualízalo a mano aquí.",
                        style = small, color = soft,
                    )
                    Button(
                        onClick = { vm.updateEngine() },
                        enabled = !vm.updatingEngine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        if (vm.updatingEngine) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Actualizando…")
                        } else {
                            Text("Actualizar ahora")
                        }
                    }
                }
            }
        }

        item {
            InfoCard(title = "Dónde se guardan") {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Videos → Movies/Bajalo", style = small, color = soft)
                    Text("Fotos → Pictures/Bajalo", style = small, color = soft)
                    Text("Audios → Music/Bajalo", style = small, color = soft)
                    Text("Estados de WhatsApp → dentro de esas mismas carpetas, en «WhatsApp»", style = small, color = soft)
                }
            }
        }

        item {
            InfoCard(title = "Sobre Bajalo") {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Sin anuncios, sin rastreadores y sin crear cuentas. Hecho para uso personal.",
                        style = small, color = soft,
                    )
                    Text(
                        "Úsala con contenido tuyo o con permiso de su autor, y respeta los derechos de autor " +
                            "y las condiciones de cada plataforma. No descarga contenido con protección DRM " +
                            "(Netflix, Disney+, Prime Video, etc.).",
                        style = small, color = soft,
                    )
                    Text(
                        "Motor: yt-dlp (licencia Unlicense) y youtubedl-android.",
                        style = small, color = soft,
                    )
                }
            }
        }
    }
}
