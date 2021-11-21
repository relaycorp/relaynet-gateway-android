package tech.relaycorp.gateway.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import androidx.room.Room
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import dagger.Module
import dagger.Provides
import tech.relaycorp.awala.keystores.file.FileKeystoreRoot
import tech.relaycorp.awala.keystores.file.FileSessionPublicKeystore
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.database.AppDatabase
import tech.relaycorp.gateway.data.disk.AndroidPrivateKeyStore
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.gateway.pdc.PoWebClientProvider
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.keystores.SessionPublicKeyStore
import tech.relaycorp.relaynet.nodes.GatewayManager
import java.io.File
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module
class DataModule {

    // Database

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
        database.storedParcelDao()

    @Provides
    @Singleton
    fun parcelCollectionDao(database: AppDatabase) =
        database.parcelCollectionDao()

    @Provides
    @Singleton
    fun localEndpointDao(database: AppDatabase) =
        database.localEndpointDao()

    // Preferences

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
    fun flowSharedPreferences(sharedPreferences: SharedPreferences) =
        FlowSharedPreferences(sharedPreferences)

    // Android Validators

    @Provides
    @Named("validator_hostname")
    fun hostnameValidator() = { hostname: String ->
        Patterns.DOMAIN_NAME.matcher(hostname).matches()
    }

    // CogRPC

    @Provides
    fun cogRPCClientBuilder(): CogRPCClient.Builder = CogRPCClient.Builder

    // PoWeb

    @Provides
    @Singleton
    fun dohClient() =
        // TODO: Remove custom DNS resolver once we can use CloudFlare's (which is the default)
        // https://github.com/cloudflare/cloudflare-docs/issues/565
        DoHClient("https://dns.google/dns-query")

    @Provides
    fun poWebClientBuilder() = object : PoWebClientBuilder {
        override suspend fun build(address: ServiceAddress) =
            PoWebClient.initRemote(address.host, address.port)
    }

    @Provides
    fun poWebClientProvider(
        publicGatewayPreferences: PublicGatewayPreferences,
        poWebClientBuilder: PoWebClientBuilder
    ) = object : PoWebClientProvider {
        override suspend fun get() =
            poWebClientBuilder.build(
                publicGatewayPreferences.getPoWebAddress()
            )
    }

    // Awala keystores

    @Provides
    @Singleton
    fun keystoreRoot(context: Context) = FileKeystoreRoot(File(context.filesDir, "keystores"))

    @Provides
    @Singleton
    fun privateKeyStore(
        context: Context,
        keystoreRoot: Provider<FileKeystoreRoot>
    ): PrivateKeyStore =
        AndroidPrivateKeyStore(keystoreRoot.get(), context)

    @Provides
    @Singleton
    fun publicKeyStore(keystoreRoot: Provider<FileKeystoreRoot>): SessionPublicKeyStore =
        FileSessionPublicKeystore(keystoreRoot.get())

    @Provides
    @Singleton
    fun gatewayManager(
        privateKeyStore: Provider<PrivateKeyStore>,
        publicKeyStore: Provider<SessionPublicKeyStore>
    ) =
        GatewayManager(privateKeyStore.get(), publicKeyStore.get())
}
