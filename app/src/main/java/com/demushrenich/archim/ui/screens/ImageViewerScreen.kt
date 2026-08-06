@file:Suppress("DEPRECATION")

package com.demushrenich.archim.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.imageLoader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kotlin.math.abs
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.demushrenich.archim.data.ArchiveNavigationState
import com.demushrenich.archim.domain.BackgroundMode
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.MediaType
import com.demushrenich.archim.domain.ReadingDirection
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.stringResource
import com.demushrenich.archim.R
import com.demushrenich.archim.domain.utils.ImageViewerUtils
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.delay

@Composable
fun SetStatusBarVisible(visible: Boolean) {
    val view = LocalView.current
    LaunchedEffect(visible) {
        val controller = ViewCompat.getWindowInsetsController(view) ?: return@LaunchedEffect
        if (visible) {
            controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }
}

@Composable
fun PreloadAdjacentImages(imagesList: List<ImageItem>, selectedImageIndex: Int) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader

    LaunchedEffect(selectedImageIndex) {
        listOf(selectedImageIndex - 1, selectedImageIndex + 1)
            .filter { it in imagesList.indices }
            .forEach { index ->
                val item = imagesList[index]
                if (item.mediaType != MediaType.IMAGE) return@forEach
                val request = ImageRequest.Builder(context)
                    .data(item.data ?: item.filePath)
                    .memoryCacheKey(item.id)
                    .diskCacheKey(item.id)
                    .build()
                imageLoader.enqueue(request)
            }
    }
}

private val VIDEO_SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun EmbeddedVideoPlayer(
    item: ImageItem,
    uiVisible: Boolean,
    onToggleUi: () -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            val path = item.filePath
            if (path != null) {
                setMediaItem(Media3Item.fromUri(path))
                prepare()
                playWhenReady = true
            }
        }
    }

    var isPlaying by remember(item.id) { mutableStateOf(true) }
    var isMuted by remember(item.id) { mutableStateOf(false) }
    var lastVolume by remember(item.id) { mutableFloatStateOf(1f) }
    var speed by remember(item.id) { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember(item.id) { mutableStateOf(false) }
    var position by remember(item.id) { mutableLongStateOf(0L) }
    var duration by remember(item.id) { mutableLongStateOf(0L) }

    DisposableEffect(item.id) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(item.id) {
        while (true) {
            position = exoPlayer.currentPosition.coerceAtLeast(0)
            duration = exoPlayer.duration.coerceAtLeast(0)
            delay(500)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(item.id) {
                    detectTapGestures(onTap = { onToggleUi() })
                }
        )

        if (uiVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Slider(
                    value = position.toFloat(),
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    onValueChange = { exoPlayer.seekTo(it.toLong()) }
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        IconButton(onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            isMuted = !isMuted
                            if (isMuted) {
                                lastVolume = exoPlayer.volume
                                exoPlayer.volume = 0f
                            } else {
                                exoPlayer.volume = lastVolume
                            }
                        }) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    Box {
                        TextButton(onClick = { showSpeedMenu = true }) {
                            Text("${speed}x", color = Color.White)
                        }
                        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                            VIDEO_SPEED_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option}x") },
                                    onClick = {
                                        speed = option
                                        exoPlayer.setPlaybackSpeed(option)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    state: AppUiState,
    archiveNavState: ArchiveNavigationState?,
    viewerImages: List<ImageItem>,
    onBack: () -> Unit,
    onIndexChange: (Int) -> Unit,
    readingDirection: ReadingDirection,
    onBackgroundModeChange: (BackgroundMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiVisible by remember { mutableStateOf(true) }
    var displayText by remember { mutableStateOf("") }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    var backgroundMode by remember { mutableStateOf(state.settings.backgroundMode) }
    LaunchedEffect(state.settings.backgroundMode) {
        backgroundMode = state.settings.backgroundMode
    }

    val imagesList = remember(viewerImages) {
        ImageViewerUtils.filterImages(viewerImages)
    }

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isAnimating) 0f else dragOffsetX,
        animationSpec = tween(300),
        finishedListener = {
            isAnimating = false
            dragOffsetX = 0f
        }
    )

    val switchBackgroundMode = {
        val newMode = when (backgroundMode) {
            BackgroundMode.SYSTEM -> BackgroundMode.BLACK
            BackgroundMode.BLACK -> BackgroundMode.WHITE
            BackgroundMode.WHITE -> BackgroundMode.SYSTEM
        }
        backgroundMode = newMode
        onBackgroundModeChange(newMode)
    }

    val backgroundColor = when (backgroundMode) {
        BackgroundMode.SYSTEM -> Color.Transparent
        BackgroundMode.BLACK -> Color.Black
        BackgroundMode.WHITE -> Color.White
    }

    val filteredSelectedImageIndex = state.imageView.selectedImageIndex

    LaunchedEffect(filteredSelectedImageIndex, imagesList) {
        val selectedIndex = filteredSelectedImageIndex ?: return@LaunchedEffect
        if (selectedIndex !in imagesList.indices) return@LaunchedEffect

        val currentImage = imagesList[selectedIndex]

        scope.launch {
            ImageViewerUtils.updateProgress(
                context = context,
                archiveUri = state.archive.currentArchiveUri,
                currentImage = currentImage,
                archiveNavState = archiveNavState
            )
        }

        displayText = ImageViewerUtils.calculateDisplayText(
            context = context,
            archiveUri = state.archive.currentArchiveUri,
            archiveNavState = archiveNavState,
            imagesList = imagesList,
            selectedIndex = selectedIndex
        )
    }

    PreloadAdjacentImages(imagesList, filteredSelectedImageIndex ?: 0)



    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val selectedIndex = filteredSelectedImageIndex ?: return

        if (selectedIndex >= imagesList.size || selectedIndex < 0) {
            LaunchedEffect(selectedIndex) {
                onBack()
            }
            return
        }

        val currentImage = imagesList[selectedIndex]
        val showUI = if (scale <= 1f) uiVisible else false

        SetStatusBarVisible(showUI)

        if (currentImage.mediaType == MediaType.VIDEO) {
            EmbeddedVideoPlayer(
                item = currentImage,
                uiVisible = uiVisible,
                onToggleUi = { uiVisible = !uiVisible }
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(currentImage.data ?: currentImage.filePath)
                        .memoryCacheKey(currentImage.id)
                        .build()
                ),
                contentDescription = currentImage.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedIndex) {
                        awaitPointerEventScope {
                            var totalDragX = 0f
                            var isSwiping = false
                            var tapStartTime = 0L
                            var tapDetected = false
                            var lastTapTime = 0L
                            val doubleTapTimeout = 250L

                            while (true) {
                                val event = awaitPointerEvent()
                                val pointers = event.changes.size

                                if (pointers == 2) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (zoomChange != 1f) {
                                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                                    }

                                    if (scale > 1f) {
                                        offsetX += panChange.x * 0.3f
                                        offsetY += panChange.y * 0.3f
                                        val maxOffsetX = size.width * (scale - 1) / 2
                                        val maxOffsetY = size.height * (scale - 1) / 2
                                        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                    }
                                } else if (pointers == 1) {
                                    val change = event.changes.first()

                                    if (change.pressed && tapStartTime == 0L) {
                                        tapStartTime = System.currentTimeMillis()
                                        tapDetected = true
                                    }

                                    if (scale > 1.2f) {
                                        val dragChangeX = change.positionChange().x
                                        val dragChangeY = change.positionChange().y

                                        offsetX += dragChangeX * 0.3f
                                        offsetY += dragChangeY * 0.3f
                                        val maxOffsetX = size.width * (scale - 1) / 2
                                        val maxOffsetY = size.height * (scale - 1) / 2
                                        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)

                                        if (abs(dragChangeX) > 2f || abs(dragChangeY) > 2f) {
                                            tapDetected = false
                                        }
                                    } else {
                                        val dragChangeX = change.positionChange().x

                                        if (abs(dragChangeX) > 2f) {
                                            tapDetected = false
                                            totalDragX += dragChangeX

                                            if (abs(totalDragX) > 20f) {
                                                isSwiping = true
                                                dragOffsetX = (totalDragX * 0.22f).coerceIn(
                                                    -size.width * 0.08f,
                                                    size.width * 0.08f
                                                )
                                            }
                                        }
                                    }

                                    if (!change.pressed) {
                                        val tapDuration = System.currentTimeMillis() - tapStartTime

                                        if (tapDetected && tapDuration < 200 && !isSwiping) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastTapTime < doubleTapTimeout) {
                                                if (scale > 1f) {
                                                    scale = 1f
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                    dragOffsetX = 0f
                                                    uiVisible = true
                                                } else {
                                                    scale = 2f
                                                }
                                            }
                                            if (scale <= 1f) {
                                                uiVisible = !uiVisible
                                            }
                                            lastTapTime = now
                                        } else if (isSwiping && scale <= 1.2f) {
                                            val threshold = 200f
                                            if (readingDirection == ReadingDirection.LEFT_TO_RIGHT) {
                                                if (totalDragX > threshold && selectedIndex > 0) {
                                                    onIndexChange(selectedIndex - 1)
                                                }
                                                if (totalDragX < -threshold && selectedIndex < imagesList.size - 1) {
                                                    onIndexChange(selectedIndex + 1)
                                                }
                                            } else {
                                                if (totalDragX > threshold && selectedIndex < imagesList.size - 1) {
                                                    onIndexChange(selectedIndex + 1)
                                                }
                                                if (totalDragX < -threshold && selectedIndex > 0) {
                                                    onIndexChange(selectedIndex - 1)
                                                }
                                            }
                                        }

                                        totalDragX = 0f
                                        dragOffsetX = 0f
                                        isSwiping = false
                                        tapStartTime = 0L
                                        tapDetected = false
                                    }
                                }

                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .scale(scale)
                    .offset(x = (offsetX + animatedOffsetX).dp, y = offsetY.dp)
            )
        }

        if (showUI) {
            TopAppBar(
                title = {
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Text(
                            text = currentImage.fileName,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = switchBackgroundMode) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = stringResource(R.string.change_background),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            Text(
                displayText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}