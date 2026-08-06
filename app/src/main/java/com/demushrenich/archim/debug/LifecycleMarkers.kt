package com.demushrenich.archim.debug

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

object LifecycleMarkers {

    private const val TAG = "AppLifecycle"
    private var attached = false

    fun attach(context: Context) {
        if (attached) return
        attached = true

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Log.d(TAG, ">>> APP FOREGROUND (onStart) <<<")
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.d(TAG, ">>> APP BACKGROUND (onStop) <<<")
            }

            override fun onResume(owner: LifecycleOwner) {
                Log.d(TAG, ">>> APP RESUMED <<<")
            }

            override fun onPause(owner: LifecycleOwner) {
                Log.d(TAG, ">>> APP PAUSED <<<")
            }
        })
    }
}