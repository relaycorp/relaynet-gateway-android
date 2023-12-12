package tech.relaycorp.gateway

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.conscrypt.Conscrypt
import tech.relaycorp.gateway.background.ForegroundAppMonitor
import tech.relaycorp.gateway.background.publicsync.PublicSyncWorker
import tech.relaycorp.gateway.background.publicsync.PublicSyncWorkerFactory
import tech.relaycorp.gateway.common.Logging
import tech.relaycorp.gateway.common.di.AppComponent
import tech.relaycorp.gateway.common.di.DaggerAppComponent
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.publicsync.PublicSync
import java.security.Security
import java.time.Duration
import java.util.logging.Level
import java.util.logging.LogManager
import javax.inject.Inject

open class App : Application() {

    open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

    val mode by lazy {
        try {
            classLoader.loadClass("tech.relaycorp.gateway.AppTest")
            Mode.Test
        } catch (e: ClassNotFoundException) {
            Mode.Normal
        }
    }

    @VisibleForTesting
    val backgroundScope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var foregroundAppMonitor: ForegroundAppMonitor

    @Inject
    lateinit var localConfig: LocalConfig

    @Inject
    lateinit var publicSync: PublicSync

    @Inject
    lateinit var publicSyncWorkerFactory: PublicSyncWorkerFactory

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        setupTLSProvider()
        setupLogger()

        enqueuePublicSyncWorker()

        if (BuildConfig.DEBUG && mode != Mode.Test) {
            StrictModeSetup(this)
        }

        backgroundScope.launch {
            bootstrapGateway()
            launch { startPublicSyncWhenPossible() }
            launch { deleteExpiredCertificates() }
        }

        registerActivityLifecycleCallbacks(foregroundAppMonitor)
    }

    private fun setupLogger() {
        LogManager.getLogManager()
        Logging.level = if (BuildConfig.DEBUG) Level.ALL else Level.WARNING
    }

    private fun setupTLSProvider() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    private suspend fun bootstrapGateway() {
        if (mode != Mode.Test) {
            localConfig.bootstrap()
        }
    }

    protected open suspend fun startPublicSyncWhenPossible() {
        publicSync.sync()
    }

    private suspend fun deleteExpiredCertificates() {
        localConfig.deleteExpiredCertificates()
    }

    protected open fun enqueuePublicSyncWorker() {
        WorkManager.initialize(
            this@App,
            Configuration.Builder()
                .setWorkerFactory(publicSyncWorkerFactory)
                .build(),
        )
        WorkManager.getInstance(this@App)
            .enqueueUniquePeriodicWork(
                "public-sync",
                ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequestBuilder<PublicSyncWorker>(PUBLIC_SYNC_WORKER_PERIOD)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .build(),
            )
    }

    enum class Mode { Normal, Test }

    companion object {
        private val PUBLIC_SYNC_WORKER_PERIOD =
            Duration.ofMillis(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
    }
}
