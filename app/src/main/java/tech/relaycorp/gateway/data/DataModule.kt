package tech.relaycorp.gateway.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.database.AppDatabase
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import javax.inject.Named
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun database(context: Context, appMode: App.Mode): AppDatabase =
        when (appMode) {
            App.Mode.Normal ->
                Room.databaseBuilder(context, AppDatabase::class.java, "gateway")
            App.Mode.Test ->
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        }.build()

    @Provides
    @Singleton
    fun storedParcelDao(database: AppDatabase) =
        database.parcelRepository()

    @Provides
    @Named("preferences_name")
    fun preferencesName(appMode: App.Mode) =
        when (appMode) {
            App.Mode.Normal -> "pref_gateway"
            App.Mode.Test -> "pref_gateway_test"
        }

    @Provides
    fun sharedPreferences(
        context: Context,
        @Named("preferences_name") preferencesName: String
    ): SharedPreferences =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    @Provides
    fun cogRPCClientBuilder(): CogRPCClient.Builder = CogRPCClient.Builder
}
