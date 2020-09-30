package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.gateway.test.factory.ParcelFactory
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.poweb.ServerConnectionException
import tech.relaycorp.relaynet.bindings.pdc.NonceSigner
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.testing.CertificationPath
import tech.relaycorp.relaynet.testing.KeyPairSet
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import javax.inject.Inject

class GatewaySyncServiceParcelCollectionTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Inject
    lateinit var sensitiveStore: SensitiveStore

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
        setGatewayCertificate(CertificationPath.PRIVATE_GW)
        val parcel = ParcelFactory.buildSerialized()
        val storeResult = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(storeResult is StoreParcel.Result.Success)

        val parcelCollection =
            PoWebClient.initLocal(PDCServer.PORT)
                .collectParcels(
                    arrayOf(
                        NonceSigner(
                            CertificationPath.PRIVATE_ENDPOINT,
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
        setGatewayCertificate(CertificationPath.PRIVATE_GW)
        val parcel = ParcelFactory.buildSerialized()
        val storeResult = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(storeResult is StoreParcel.Result.Success)

        val parcelCollection =
            PoWebClient.initLocal(PDCServer.PORT)
                .collectParcels(
                    arrayOf(
                        NonceSigner(
                            CertificationPath.PRIVATE_ENDPOINT,
                            KeyPairSet.PUBLIC_GW.private // Invalid key to trigger invalid handshake
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

    private suspend fun setGatewayCertificate(cert: Certificate) {
        sensitiveStore.store("local_gateway.certificate", cert.serialize())
    }
}
