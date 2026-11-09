package com.arthurnagy.dexlens

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt's application: the singleton graph (repositories, DataStore, every feature's entry builders)
 * lives as long as the process, so screens, services and workers share it. Registered as
 * `android:name=".MainApplication"` in the manifest.
 */
@HiltAndroidApp
class MainApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
