package tech.relaycorp.gateway

import android.content.Context
import android.content.res.Resources
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
}
