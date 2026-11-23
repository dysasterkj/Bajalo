@file:OptIn(ExperimentalMaterial3Api::class)

package com.kjota.bajalo.ui

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kjota.bajalo.MainViewModel
import com.kjota.bajalo.Tabs
import com.kjota.bajalo.data.LibraryItem
import com.kjota.bajalo.download.DownloadCenter
import com.kjota.bajalo.download.TaskStatus

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val tasks by DownloadCenter.tasks.collectAsStateWithLifecycle()
    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    var toDelete by remember { mutableStateOf<LibraryItem?>(null) }

    LaunchedEffect(vm.tab, doneCount) {
        if (vm.tab == Tabs.LIBRARY) vm.refreshLibrary()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
    ) {
        Text(
            text = "Mis descargas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Todo se guarda también en tu galería, en la carpeta Bajalo.",
            style = MaterialTheme.typography.bodyMedium,
            color = Glass.Soft,
        )

        if (vm.library.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Aún no has descargado nada.\nLo que descargues aparecerá aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Glass.Soft,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vm.library, key = { it.uri.toString() }) { item ->
                    LibraryRow(
                        item = item,
                        onOpen = { openMedia(ctx, item.uri, item.mime) },
                        onShare = { shareMedia(ctx, item.uri, item.mime) },
                        onDelete = { toDelete = item },
                    )
                }
            }
        }
    }

    val pending = toDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar archivo?") },
            text = { Text(pending.name) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteItem(pending)
                    toDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun LibraryRow(
    item: LibraryItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.isAudio) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Audio",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else {
                Thumb(item.uri, Modifier.size(64.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val whenText = DateUtils.getRelativeTimeSpanString(
                    item.dateAdded * 1000,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
                val size = formatSize(item.size)
                Text(
                    text = if (size.isEmpty()) whenText else "$size · $whenText",
                    style = MaterialTheme.typography.bodySmall,
                    color = Glass.Soft,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Compartir")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
