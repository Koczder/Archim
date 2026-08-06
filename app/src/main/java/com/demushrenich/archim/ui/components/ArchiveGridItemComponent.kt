package com.demushrenich.archim.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.domain.CornerStyle
import com.demushrenich.archim.domain.utils.archiveFormat
import com.demushrenich.archim.domain.utils.buildPreviewImageRequest
import com.demushrenich.archim.domain.utils.previewCacheKey
import java.io.File

@Composable
fun ArchiveGridItemComponent(
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
        CornerStyle.ROUNDED -> RoundedCornerShape(8.dp)
        CornerStyle.SQUARE -> RoundedCornerShape(0.dp)
    }

    val percent = archive.readingProgress?.getProgressPercentage()
    val isDone = percent != null && percent >= 1f

    Column(modifier = modifier.fillMaxWidth().padding(6.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            shape = cardShape
        ) {
            Box(Modifier.fillMaxSize()) {
                if (hasPreviewFile && painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = archive.displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = archiveFormat(archive),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isDone) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (percent != null && !isDone) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = archive.displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = formatFileSize(archive.fileSize),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}