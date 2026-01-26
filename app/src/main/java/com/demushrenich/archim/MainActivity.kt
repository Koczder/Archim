package com.demushrenich.archim

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.demushrenich.archim.data.managers.SettingsManager
import com.demushrenich.archim.domain.utils.clearCacheDir
import com.demushrenich.archim.domain.utils.clearLargeArchiveCache
import com.demushrenich.archim.ui.compose.AppContent
import com.demushrenich.archim.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager(this)
        settingsManager.initializeLanguage()

        setupImageLoader()
        cleanupCacheIfNeeded(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()

            LaunchedEffect(Unit) {
                viewModel.initializeSettings(this@MainActivity)
            }

            AppContent(
                viewModel = viewModel,
                settingsManager = settingsManager
            )
        }
    }

    private fun setupImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun cleanupCacheIfNeeded(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                clearLargeArchiveCache(this@MainActivity)
                clearCacheDir(this@MainActivity)
            }
        }
    }
}