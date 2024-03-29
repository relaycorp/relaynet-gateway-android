package tech.relaycorp.gateway.background.endpoint

import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.gateway.test.KeystoreResetTestRule
import tech.relaycorp.gateway.test.factory.ParcelFactory
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import javax.inject.Inject

class GatewaySyncServiceParcelCollectionTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @get:Rule
    val keystoreResetRule = KeystoreResetTestRule()

    @Inject
    lateinit var privateKeyStore: PrivateKeyStore

    @Inject
    lateinit var storeParcel: StoreParcel

    private val coroutineContext
        get() = (getApplicationContext() as App).backgroundContext

    @Before
    fun setUp() {
        AppTestProvider.component.inject(this)
        serviceRule.bindService(Intent(getApplicationContext(), GatewaySyncService::class.java))
    }

    @After
    fun tearDown() {
        serviceRule.unbindService()
        Thread.sleep(3000) // Wait for netty to properly stop, to avoid a RejectedExecutionException
    }

    @Test
    fun parcelCollection_receiveParcel() = runTest(coroutineContext) {
        val parcel = ParcelFactory.buildSerialized()
        val storeResult = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(storeResult is StoreParcel.Result.Success)

        val parcelCollection =
            PoWebClient.initLocal(PDCServer.PORT)
                .collectParcels(
                    arrayOf(
                        Signer(
                            PDACertPath.PRIVATE_ENDPOINT,
                            KeyPairSet.PRIVATE_ENDPOINT.private,
                        ),
                    ),
                    StreamingMode.KeepAlive,
                ).take(1).first()

        assertEquals(
            Parcel.deserialize(parcel).id,
            Parcel.deserialize(parcelCollection.parcelSerialized).id,
        )
    }

    @Test(expected = ServerConnectionException::class)
    fun parcelCollection_invalidHandshake() = runTest(coroutineContext) {
        val parcel = ParcelFactory.buildSerialized()
        val storeResult = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(storeResult is StoreParcel.Result.Success)

        PoWebClient.initLocal(PDCServer.PORT)
            .collectParcels(
                arrayOf(
                    Signer(
                        PDACertPath.PRIVATE_ENDPOINT,
                        // Invalid key to trigger invalid handshake
                        KeyPairSet.INTERNET_GW.private,
                    ),
                ),
                StreamingMode.CloseUponCompletion,
            )
            .collect()
    }
}
