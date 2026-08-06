package com.demushrenich.archim.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import com.demushrenich.archim.R
import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.domain.CornerStyle
import com.demushrenich.archim.domain.utils.archiveFormat
import com.demushrenich.archim.domain.utils.buildPreviewImageRequest
import com.demushrenich.archim.domain.utils.previewCacheKey
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.2f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}

@Composable
fun ArchiveItemComponent(
    archive: ArchiveInfo,
    isPreviewAlreadyLoaded: Boolean,
    onPreviewLoaded: (String) -> Unit,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    imageLoader: ImageLoader = LocalContext.current.imageLoader,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewPath = archive.previewPath
    val hasPreviewFile = previewPath != null && File(previewPath).exists()

    val painter = if (hasPreviewFile) {
        rememberAsyncImagePainter(
            model = buildPreviewImageRequest(context, previewPath!!),
            imageLoader = imageLoader,
            onSuccess = { onPreviewLoaded(previewCacheKey(previewPath)) }
        )
    } else null

    val isImageLoaded = isPreviewAlreadyLoaded ||
            (painter != null && painter.state is AsyncImagePainter.State.Success)

    val alpha by animateFloatAsState(
        targetValue = if (isImageLoaded) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isImageLoaded) 1f else 0.9f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "scale"
    )

    val cardShape = when (cornerStyle) {
        CornerStyle.ROUNDED -> RoundedCornerShape(4.dp)
        CornerStyle.SQUARE -> RoundedCornerShape(0.dp)
    }

    Row(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        if (hasPreviewFile && painter != null) {
            Card(
                modifier = Modifier.width(80.dp).height(100.dp),
                shape = cardShape
            ) {
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = painter,
                        contentDescription = archive.displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.width(80.dp).height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = cardShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = archiveFormat(archive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = archive.displayName,
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dateText = if (archive.lastModified > 0) {
                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(archive.lastModified))
                } else "—"
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatFileSize(archive.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            archive.readingProgress?.let { progress ->
                val percent = progress.getProgressPercentage()

                if (percent >= 1f) {
                    Text(
                        text = stringResource(R.string.read_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${progress.currentIndex} / ${progress.totalImages}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } ?: run {
                LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}