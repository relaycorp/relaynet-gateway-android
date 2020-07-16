package tech.relaycorp.gateway

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.conscrypt.Conscrypt
import tech.relaycorp.gateway.background.publicsync.PublicSyncWorker
import tech.relaycorp.gateway.background.publicsync.PublicSyncWorkerFactory
import tech.relaycorp.gateway.common.Logging
import tech.relaycorp.gateway.common.di.AppComponent
import tech.relaycorp.gateway.common.di.DaggerAppComponent
import java.security.Security
import java.time.Duration
import java.util.logging.Level
import java.util.logging.LogManager
import javax.inject.Inject

class App : Application() {

    val component: AppComponent by lazy {
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

    @Inject
    lateinit var publicSyncWorkerFactory: PublicSyncWorkerFactory

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        setupTLSProvider()
        setupLogger()
        setupStrictMode()
        enqueuePublicSyncWorker()
    }

    private fun setupLogger() {
        LogManager.getLogManager()
        Logging.level = if (BuildConfig.DEBUG) Level.ALL else Level.WARNING
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG && mode != Mode.Test) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build()
            )
            StrictMode.setVmPolicy(
                /*
                  To disable the some of the checks we need to manually set all checks.
                  This code is based on the `detectAll()` implementation.
                  Checks disabled:
                  - UntaggedSockets (we aren't able to tag Netty socket threads)
                  - CleartextNetwork (it's considering gRPC over TLS communication as cleartext)
                 */
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectFileUriExposure()
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detectContentUriWithoutPermission()
                        }
                    }
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            detectCredentialProtectedWhileLocked()
                        }
                    }
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
    }

    private fun setupTLSProvider() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    private fun enqueuePublicSyncWorker() {
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(publicSyncWorkerFactory)
                .build()
        )
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "public-sync",
                ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequestBuilder<PublicSyncWorker>(PUBLIC_SYNC_WORKER_PERIOD)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
    }

    enum class Mode { Normal, Test }

    companion object {
        private val PUBLIC_SYNC_WORKER_PERIOD = Duration.ofHours(1)
    }
}
