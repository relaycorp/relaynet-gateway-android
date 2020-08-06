package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.messages.control.ClientRegistrationAuthorization
import java.time.ZonedDateTime
import javax.inject.Inject

class EndpointRegistration
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao,
    private val localConfig: LocalConfig
) {
    suspend fun authorize(endpointApplicationId: String): ByteArray {
        val expiryDate = ZonedDateTime.now().plusSeconds(5)
        val cra = ClientRegistrationAuthorization(expiryDate, endpointApplicationId.toByteArray())
        return cra.serialize(localConfig.getKeyPair().private)
    }

    suspend fun register(applicationId: String, endpointAddress: MessageAddress) {
        localEndpointDao.insert(LocalEndpoint(endpointAddress, applicationId))
    }
}
