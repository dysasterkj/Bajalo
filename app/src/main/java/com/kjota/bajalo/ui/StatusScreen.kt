@file:OptIn(ExperimentalMaterial3Api::class)

package com.kjota.bajalo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kjota.bajalo.MainViewModel
import com.kjota.bajalo.Tabs
import com.kjota.bajalo.data.StatusFile
import com.kjota.bajalo.data.WaApp
import com.kjota.bajalo.data.WhatsAppStatuses

@Composable
fun StatusScreen(vm: MainViewModel) {
    LaunchedEffect(vm.tab, vm.waApp) {
        if (vm.tab == Tabs.STATUS) vm.loadStatuses()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.onWaFolderPicked(uri)
    }
    val connect = { picker.launch(WhatsAppStatuses.initialPickerUri(vm.waApp)) }

    var menuFile by remember { mutableStateOf<StatusFile?>(null) }
    var viewing by remember { mutableStateOf<StatusFile?>(null) }

    menuFile?.let { f ->
        ModalBottomSheet(
            onDismissRequest = { menuFile = null },
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (f.isVideo) "Estado en video" else "Estado en foto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(
                    onClick = { viewing = f; menuFile = null },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Ver") }
                OutlinedButton(
                    onClick = { vm.saveStatuses(listOf(f)); menuFile = null },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Descargar") }
            }
        }
    }

    viewing?.let { f ->
        StatusViewer(
            file = f,
            onClose = { viewing = null },
            onSave = { vm.saveStatuses(listOf(f)) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
    ) {
        Text(
            text = "Estados de WhatsApp",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Toca un estado para verlo o descargarlo. Mantén pulsado para elegir varios.",
            style = MaterialTheme.typography.bodyMedium,
            color = Glass.Soft,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WaApp.values().forEach { app ->
                FilterChip(
                    selected = vm.waApp == app,
                    onClick = { vm.switchWa(app) },
                    label = { Text(app.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            !vm.waLinked -> ConnectCard(onConnect = { connect() })

            vm.waLoading && vm.waFiles.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            vm.waFiles.isEmpty() -> EmptyCard(
                onRefresh = { vm.loadStatuses() },
                onReconnect = { connect() },
            )

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(vm.waFiles, key = { it.uri.toString() }) { f ->
                        StatusCell(
                            file = f,
                            selected = f.uri in vm.waSelected,
                            saved = f.name in vm.waSaved,
                            onClick = {
                                // Con estados ya elegidos, tocar suma o quita; si no, abre el menú.
                                if (vm.waSelected.isNotEmpty()) vm.toggleSelect(f.uri) else menuFile = f
                            },
                            onLongClick = { vm.toggleSelect(f.uri) },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { vm.selectAllStatuses() }) {
                        Text(if (vm.waSelected.size == vm.waFiles.size) "Quitar" else "Todos")
                    }
                    Button(
                        onClick = { vm.saveSelectedStatuses() },
                        enabled = vm.waSelected.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            if (vm.waSelected.isEmpty()) "Mantén pulsado para elegir varios"
                            else "Guardar (${vm.waSelected.size})"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusCell(
    file: StatusFile,
    selected: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Thumb(file.uri, Modifier.fillMaxSize())

        if (file.isVideo) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            )
            Icon(
                Icons.Filled.Check,
                contentDescription = "Elegido",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(3.dp),
            )
        }

        if (saved) {
            Text(
                text = "Guardado",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ConnectCard(onConnect: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = Glass.Teal) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Conecta la carpeta de estados (una sola vez)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val style = MaterialTheme.typography.bodyMedium
            val color = Glass.Soft
            Text("1. Abre WhatsApp y mira los estados que quieres guardar.", style = style, color = color)
            Text("2. Vuelve aquí y toca «Conectar carpeta».", style = style, color = color)
            Text(
                "3. En la pantalla que se abre toca «Usar esta carpeta» y luego «Permitir».",
                style = style, color = color,
            )
            Text(
                "Bajalo solo lee esa carpeta dentro de tu teléfono. No envía nada a internet.",
                style = MaterialTheme.typography.bodySmall, color = color,
            )
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("Conectar carpeta") }
        }
    }
}

@Composable
private fun EmptyCard(onRefresh: () -> Unit, onReconnect: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = Glass.Teal) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Aún no hay estados para guardar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Abre WhatsApp, mira un estado y vuelve aquí. WhatsApp solo deja guardar los que ya viste.",
                style = MaterialTheme.typography.bodyMedium,
                color = Glass.Soft,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("Actualizar") }
                TextButton(onClick = onReconnect) { Text("Reconectar carpeta") }
            }
        }
    }
}
