package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.RejectedParcelException
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.testing.CertificationPath
import tech.relaycorp.relaynet.testing.KeyPairSet
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import javax.inject.Inject

class GatewaySyncServiceParcelDeliveryTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val endpointSigner =
        Signer(CertificationPath.PRIVATE_ENDPOINT, KeyPairSet.PRIVATE_ENDPOINT.private)

    @Inject
    lateinit var sensitiveStore: SensitiveStore

    @Inject
    lateinit var storedParcelDao: StoredParcelDao

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
    fun parcelDelivery_validParcel() = runBlocking {
        setGatewayCertificate(CertificationPath.PRIVATE_GW)
        val recipient = "https://example.org"

        val parcel = Parcel(
            recipient,
            ByteArray(0),
            CertificationPath.PRIVATE_ENDPOINT,
            senderCertificateChain = setOf(CertificationPath.PRIVATE_GW)
        ).serialize(KeyPairSet.PRIVATE_ENDPOINT.private)

        PoWebClient.initLocal(PDCServer.PORT).deliverParcel(parcel, endpointSigner)

        val storedParcels = storedParcelDao.listForRecipients(
            listOf(MessageAddress.of(recipient)),
            RecipientLocation.ExternalGateway
        ).first()
        assertEquals(1, storedParcels.size)
    }

    @Test(expected = RejectedParcelException::class)
    fun parcelDelivery_invalidParcel() = runBlocking {
        val parcel = Parcel(
            "https://example.org",
            ByteArray(0),
            CertificationPath.PUBLIC_GW // Wrong certificate to make this parcel invalid
        ).serialize(KeyPairSet.PUBLIC_GW.private)

        PoWebClient.initLocal(PDCServer.PORT).deliverParcel(parcel, endpointSigner)
    }

    private suspend fun setGatewayCertificate(cert: Certificate) {
        sensitiveStore.store("local_gateway.certificate", cert.serialize())
    }
}
