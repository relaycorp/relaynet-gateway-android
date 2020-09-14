package tech.relaycorp.gateway.ui.main

import io.bloco.faker.Faker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.courier.ProcessParcels
import tech.relaycorp.relaynet.messages.Parcel
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

class SendEchoTest
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao,
    private val localConfig: LocalConfig,
    private val storeParcel: StoreParcel,
    private val processParcels: ProcessParcels
) {

    suspend fun send() {
        withContext(Dispatchers.IO) {
                localEndpointDao
                    .list()
                    .map { endpoint ->
                        Parcel(
                            recipientAddress = endpoint.address.value,
                            payload = randomMessage().toByteArray(Charset.forName("UTF-8")),
                            senderCertificate = localConfig.getCertificate(),
                            messageId = UUID.randomUUID().toString(),
                            creationDate = ZonedDateTime.now(),
                            ttl = 3600
                        )
                    }
                    .map { parcel ->
                        storeParcel.store(
                            parcel.serialize(localConfig.getKeyPair().private),
                            RecipientLocation.LocalEndpoint
                        )
                    }

            processParcels.process()
        }
    }

    private fun randomMessage() = Faker().lorem.sentence()
}
