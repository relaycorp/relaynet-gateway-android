package tech.relaycorp.gateway

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import dagger.Module
import dagger.Provides

@Module
class AppModule(
    private val app: App
) {

    @Provides
    fun app() = app

    @Provides
    fun appMode() = app.mode

    @Provides
    fun context(): Context = app

    @Provides
    fun resources(): Resources = app.resources

    @Provides
    fun packageManager(): PackageManager = app.packageManager

    @Provides
    fun connectivityManager() =
        app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun wifiManager() =
        app.getSystemService(Context.WIFI_SERVICE) as WifiManager
}
