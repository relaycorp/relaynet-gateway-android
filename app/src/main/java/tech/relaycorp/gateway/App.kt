package tech.relaycorp.gateway

import android.app.Application
import android.os.Build
import android.os.StrictMode
import org.conscrypt.Conscrypt
import tech.relaycorp.gateway.common.Logging
import tech.relaycorp.gateway.common.di.AppComponent
import tech.relaycorp.gateway.common.di.DaggerAppComponent
import java.security.Security
import java.util.logging.Level
import java.util.logging.LogManager

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

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        setupTLSProvider()
        setupLogger()
        // TODO: Restore
//        setupStrictMode()
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

    enum class Mode { Normal, Test }
}
