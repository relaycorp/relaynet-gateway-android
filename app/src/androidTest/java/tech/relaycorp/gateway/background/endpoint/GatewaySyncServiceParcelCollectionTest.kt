package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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

    @Before
    fun setUp() {
        AppTestProvider.component.inject(this)
        serviceRule.bindService(
            Intent(
                getApplicationContext<Context>(),
                GatewaySyncService::class.java
            )
        )
    }

    @Test
    fun parcelCollection_receiveParcel() = runBlocking {
        val parcel = ParcelFactory.buildSerialized()
        val storeResult = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(storeResult is StoreParcel.Result.Success)

        val parcelCollection =
            PoWebClient.initLocal(PDCServer.PORT)
                .collectParcels(
                    arrayOf(
                        Signer(
                            PDACertPath.PRIVATE_ENDPOINT,
                            KeyPairSet.PRIVATE_ENDPOINT.private
                        )
                    ),
                    StreamingMode.CloseUponCompletion
                )
                .first()

        assertEquals(
            Parcel.deserialize(parcel).id,
            Parcel.deserialize(parcelCollection.parcelSerialized).id
        )
    }

    @Test(expected = ServerConnectionException::class)
    fun parcelCollection_invalidHandshake() = runBlocking {
        val parcel = ParcelFactory.buildSerialized()
        val storeResult = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(storeResult is StoreParcel.Result.Success)

        PoWebClient.initLocal(PDCServer.PORT)
            .collectParcels(
                arrayOf(
                    Signer(
                        PDACertPath.PRIVATE_ENDPOINT,
                        KeyPairSet.PUBLIC_GW.private // Invalid key to trigger invalid handshake
                    )
                ),
                StreamingMode.CloseUponCompletion
            )
            .collect()
    }
}
