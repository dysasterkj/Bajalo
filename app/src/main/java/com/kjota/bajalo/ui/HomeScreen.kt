@file:OptIn(ExperimentalMaterial3Api::class)

package com.kjota.bajalo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.kjota.bajalo.Tabs
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kjota.bajalo.HomeState
import com.kjota.bajalo.MainViewModel
import com.kjota.bajalo.R
import com.kjota.bajalo.download.DownloadCenter
import com.kjota.bajalo.download.DownloadTask
import com.kjota.bajalo.download.TaskStatus
import com.kjota.bajalo.engine.Engine
import com.kjota.bajalo.engine.EngineState
import com.kjota.bajalo.engine.MediaEntry

@Composable
fun HomeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tasks by DownloadCenter.tasks.collectAsStateWithLifecycle()
    val engine by Engine.state.collectAsStateWithLifecycle()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Con o sin permiso de notificaciones, la descarga se inicia igual.
        vm.startDownload()
    }

    val requestDownload: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startDownload()
        }
    }

    LaunchedEffect(Unit) {
        vm.refreshAccounts()
        vm.refreshLibrary()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header(engine) }

        item {
            LinkInput(
                vm = vm,
                engine = engine,
                onPaste = {
                    val text = clipboard.getText()?.text
                    if (!text.isNullOrBlank()) {
                        vm.urlInput = text
                        if (MainViewModel.extractUrl(text) != null) vm.analyze()
                    }
                },
            )
        }

        item { HomeStateBlock(vm = vm, engine = engine, onDownload = requestDownload) }

        if (tasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("Descargas")
                    if (tasks.any { !it.isActive }) {
                        TextButton(onClick = { vm.clearFinishedTasks() }) { Text("Limpiar") }
                    }
                }
            }
            items(tasks, key = { it.id }) { task -> TaskCard(vm, task) }
        }

        item { BentoSection(vm) }

        item { HelpCard() }
    }
}

@Composable
private fun Header(engine: EngineState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Bajalo",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFB9AFFF), Glass.Teal, Color(0xFFFF8FC0))),
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "Videos de tus redes. Gratis y sin anuncios.",
                style = MaterialTheme.typography.bodySmall,
                color = Glass.Soft,
            )
        }
        val (label, tint) = when (engine) {
            EngineState.Ready -> "Motor listo" to Glass.Teal
            EngineState.Loading -> "Preparando…" to Glass.Violet
            is EngineState.Failed -> "Sin motor" to Glass.Pink
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(tint.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LinkInput(vm: MainViewModel, engine: EngineState, onPaste: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = Glass.Violet) {
        Text(
            text = "Pega un enlace",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "YouTube, Instagram, Facebook, X y más",
            style = MaterialTheme.typography.bodySmall,
            color = Glass.Soft,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = vm.urlInput,
            onValueChange = { vm.urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Pega aquí el enlace del video") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Glass.Violet,
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                focusedContainerColor = Color.White.copy(alpha = 0.06f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
            ),
            trailingIcon = {
                IconButton(onClick = onPaste) {
                    Icon(painterResource(R.drawable.ic_paste), contentDescription = "Pegar enlace")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.analyze() }),
        )

        Spacer(Modifier.height(10.dp))
        val ready = engine is EngineState.Ready
        Button(
            onClick = { vm.analyze() },
            enabled = ready && vm.urlInput.isNotBlank() && vm.home !is HomeState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            when (engine) {
                EngineState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Preparando motor…")
                }
                is EngineState.Failed -> Text("Motor no disponible")
                EngineState.Ready -> Text("Analizar enlace")
            }
        }
    }
}

@Composable
private fun HomeStateBlock(vm: MainViewModel, engine: EngineState, onDownload: () -> Unit) {
    if (engine is EngineState.Failed) {
        ErrorCard("El motor de descargas no pudo iniciar.\n\n${engine.message}")
        return
    }
    when (val s = vm.home) {
        HomeState.Idle -> Unit
        HomeState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Text("Analizando enlace…")
        }
        is HomeState.Ready -> ResultCard(vm, s, onDownload)
        is HomeState.Error -> ErrorCard(s.message)
    }
}

@Composable
private fun ErrorCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = Color(0xFFFF6B6B)) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ResultCard(vm: MainViewModel, ready: HomeState.Ready, onDownload: () -> Unit) {
    val entries = ready.result.entries
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = Glass.Teal) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            entries.take(3).forEach { EntryRow(it) }
            if (entries.size > 3) {
                Text(
                    text = "y ${entries.size - 3} más…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Glass.Soft,
                )
            }

            if (ready.options.size > 1) {
                Text(
                    text = "Calidad",
                    style = MaterialTheme.typography.labelLarge,
                    color = Glass.Soft,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ready.options) { q ->
                        FilterChip(
                            selected = vm.quality == q,
                            onClick = { vm.quality = q },
                            label = { Text(q.label) },
                        )
                    }
                }
            }

            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_download), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (entries.size > 1) "Descargar ${entries.size} archivos" else "Descargar")
            }
        }
    }
}

@Composable
private fun EntryRow(e: MediaEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Thumb(e.thumbnail, Modifier.size(width = 96.dp, height = 64.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = e.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val sub = listOfNotNull(
                e.uploader,
                if (e.durationSec > 0) formatDuration(e.durationSec) else null,
            ).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Glass.Soft,
                )
            }
        }
    }
}

@Composable
private fun TaskCard(vm: MainViewModel, task: DownloadTask) {
    val ctx = LocalContext.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(task.thumbnail, Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                if (task.isActive) {
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = task.message,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.status == TaskStatus.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Glass.Soft
                    },
                )
            }
            when (task.status) {
                TaskStatus.QUEUED, TaskStatus.RUNNING ->
                    IconButton(onClick = { vm.cancelTask(task.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                    }

                TaskStatus.DONE ->
                    TextButton(onClick = {
                        val first = task.saved.firstOrNull()
                        if (first != null) {
                            val uri = Uri.parse(first)
                            openMedia(ctx, uri, ctx.contentResolver.getType(uri))
                        }
                    }) { Text("Abrir") }

                TaskStatus.FAILED ->
                    TextButton(onClick = { vm.retryTask(task.id) }) { Text("Reintentar") }

                TaskStatus.CANCELED ->
                    IconButton(onClick = { vm.removeTask(task.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar")
                    }
            }
        }
    }
}

@Composable
private fun HelpCard() {
    InfoCard(title = "Cómo usarlo") {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val style = MaterialTheme.typography.bodySmall
            val color = Glass.Soft
            Text(
                "• Desde Instagram, Facebook, X, YouTube o TikTok toca Compartir → Bajalo y el enlace se analiza solo.",
                style = style, color = color,
            )
            Text(
                "• Funciona con YouTube, Instagram, Facebook, X, TikTok, Reddit, Vimeo, Twitch y más de mil sitios.",
                style = style, color = color,
            )
            Text(
                "• Para historias o cuentas privadas inicia sesión en Ajustes → Cuentas.",
                style = style, color = color,
            )
            Text(
                "• Los estados de WhatsApp están en la pestaña «Estados».",
                style = style, color = color,
            )
            Text(
                "• No funciona con Netflix, Disney+, Prime Video y similares: tienen protección anticopia.",
                style = style, color = color,
            )
        }
    }
}

@Composable
private fun BentoSection(vm: MainViewModel) {
    val version by Engine.version.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp),
                tint = Glass.Teal,
                onClick = { vm.tab = Tabs.STATUS },
            ) {
                Icon(
                    painterResource(R.drawable.ic_status),
                    contentDescription = null,
                    tint = Glass.Teal,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Estados de WhatsApp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Guarda los que ya viste", style = MaterialTheme.typography.bodySmall, color = Glass.Soft)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompactTile(
                    title = "Mis descargas",
                    subtitle = "${vm.library.size} archivos",
                    tint = Glass.Violet,
                    modifier = Modifier.height(82.dp),
                    onClick = { vm.tab = Tabs.LIBRARY },
                )
                CompactTile(
                    title = "Cuentas",
                    subtitle = if (vm.loggedIn.isNotEmpty()) "Sesión activa" else "Inicia sesión",
                    tint = Glass.Blue,
                    modifier = Modifier.height(82.dp),
                    onClick = { vm.tab = Tabs.SETTINGS },
                )
            }
        }
        CompactTile(
            title = "Motor de descargas",
            subtitle = "yt-dlp ${version ?: ""} · se actualiza solo",
            tint = Glass.Pink,
            modifier = Modifier.fillMaxWidth(),
            onClick = { vm.tab = Tabs.SETTINGS },
        )
    }
}

@Composable
private fun CompactTile(
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(modifier = modifier, tint = tint, onClick = onClick) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Glass.Soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
