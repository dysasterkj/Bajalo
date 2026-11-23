package com.kjota.bajalo.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.Locale

fun formatDuration(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}

fun openMedia(ctx: Context, uri: Uri, mime: String?) {
    val i = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(i)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "No hay una app para abrir este archivo", Toast.LENGTH_SHORT).show()
    }
}

fun shareMedia(ctx: Context, uri: Uri, mime: String?) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(mime ?: "*/*")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(send, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "No se pudo compartir", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Miniatura redondeada (acepta direcciones web o content://). */
@Composable
fun Thumb(model: Any?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun InfoCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}
