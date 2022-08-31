package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.FileStore
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.RejectedParcelException
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.time.ZonedDateTime
import javax.inject.Inject

class GatewaySyncServiceParcelDeliveryTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val endpointSigner =
        Signer(PDACertPath.PRIVATE_ENDPOINT, KeyPairSet.PRIVATE_ENDPOINT.private)

    @Inject
    lateinit var fileStore: FileStore

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
        setGatewayCertificate(PDACertPath.PRIVATE_GW)
        val recipientId = "0deadbeef"
        val recipientInternetAddress = "example.org"
        val recipient = Recipient(recipientId, recipientInternetAddress)

        val parcel = Parcel(
            recipient,
            ByteArray(0),
            PDACertPath.PRIVATE_ENDPOINT,
            senderCertificateChain = setOf(PDACertPath.PRIVATE_GW)
        ).serialize(KeyPairSet.PRIVATE_ENDPOINT.private)

        PoWebClient.initLocal(PDCServer.PORT).deliverParcel(parcel, endpointSigner)

        val storedParcels = storedParcelDao.listForRecipients(
            listOf(MessageAddress.of(recipientId)),
            RecipientLocation.ExternalGateway
        ).first()
        assertEquals(1, storedParcels.size)
    }

    @Test(expected = RejectedParcelException::class)
    fun parcelDelivery_invalidParcel() = runBlocking {
        val fiveMinutesAgo = ZonedDateTime.now().minusMinutes(5)
        val recipientId = "0deadbeef"
        val recipientInternetAddress = "example.org"
        val recipient = Recipient(recipientId, recipientInternetAddress)

        val parcel = Parcel(
            recipient,
            ByteArray(0),
            PDACertPath.PRIVATE_ENDPOINT,
            creationDate = fiveMinutesAgo,
            ttl = 1
        ).serialize(KeyPairSet.PRIVATE_ENDPOINT.private)

        PoWebClient.initLocal(PDCServer.PORT).deliverParcel(parcel, endpointSigner)
    }

    private suspend fun setGatewayCertificate(cert: Certificate) {
        fileStore.store("local_gateway.certificate", cert.serialize())
    }
}
