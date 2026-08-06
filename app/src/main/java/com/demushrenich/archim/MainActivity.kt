package com.demushrenich.archim

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import com.demushrenich.archim.data.managers.SettingsManager
import com.demushrenich.archim.data.repositories.CacheRepositoryImpl
import com.demushrenich.archim.data.repositories.SettingsRepositoryImpl
import com.demushrenich.archim.debug.LogcatFileWriter
import com.demushrenich.archim.domain.repositories.CacheRepository
import com.demushrenich.archim.domain.repositories.SettingsRepository
import com.demushrenich.archim.domain.utils.clearCacheDir
import com.demushrenich.archim.domain.utils.clearLargeArchiveCache
import com.demushrenich.archim.ui.compose.AppContent
import com.demushrenich.archim.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        LogcatFileWriter.start(applicationContext)
        SettingsManager.initializeAppLocale(this)
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                clearLargeArchiveCache(applicationContext)
                clearCacheDir(applicationContext)
            }
            lifecycleScope.launch(Dispatchers.Default) {
                val imageLoader = ImageLoader.Builder(applicationContext)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                            add(SvgDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                        add(VideoFrameDecoder.Factory())
                    }
                    .build()
                Coil.setImageLoader(imageLoader)
            }
        }

        enableEdgeToEdge()

        setContent {
            MainScreen()
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val settingsRepository: SettingsRepository = remember(context) {
        SettingsRepositoryImpl(context)
    }

    val cacheRepository: CacheRepository = remember(context) {
        CacheRepositoryImpl(context)
    }

    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(settingsRepository, cacheRepository) as T
            }
        }
    )

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.toastEvents.collectLatest { event ->
                    val message = when (event) {
                        is MainViewModel.ToastEvent.PreviewsDeleted ->
                            context.getString(R.string.previews_deleted, event.count)
                        is MainViewModel.ToastEvent.CleanupError ->
                            context.getString(R.string.cleanup_error, event.message)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AppContent(viewModel = viewModel)
}