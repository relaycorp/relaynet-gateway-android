package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.ClientRegistration
import tech.relaycorp.relaynet.messages.control.ClientRegistrationAuthorization
import tech.relaycorp.relaynet.messages.control.ClientRegistrationRequest
import tech.relaycorp.relaynet.wrappers.privateAddress
import java.nio.charset.Charset
import java.time.ZonedDateTime
import javax.inject.Inject

class EndpointRegistration
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao,
    private val localConfig: LocalConfig
) {
    /**
     * Issue PNRA for an application to register one or more of its endpoints.
     */
    suspend fun authorize(endpointApplicationId: String): ByteArray {
        val expiryDate = ZonedDateTime.now().plusSeconds(AUTHORIZATION_VALIDITY_SECONDS)
        val authorization =
            ClientRegistrationAuthorization(expiryDate, endpointApplicationId.toByteArray())
        return authorization.serialize(localConfig.getKeyPair().private)
    }

    /**
     * Complete endpoint registration and return registration serialized.
     */
    @Throws(InvalidPNRAException::class)
    suspend fun register(request: ClientRegistrationRequest): ByteArray {
        val gatewayKeyPair = localConfig.getKeyPair()
        val authorization = try {
            ClientRegistrationAuthorization.deserialize(
                request.craSerialized,
                gatewayKeyPair.public
            )
        } catch (exc: InvalidMessageException) {
            throw InvalidPNRAException("Registration request contains invalid authorization", exc)
        }
        val applicationId = authorization.serverData.toString(Charset.defaultCharset())
        val endpoint = LocalEndpoint(
            MessageAddress.of(request.clientPublicKey.privateAddress),
            applicationId
        )
        localEndpointDao.insert(endpoint)

        val gatewayCertificate = localConfig.getCertificate()
        val endpointCertificate = issueEndpointCertificate(
            request.clientPublicKey,
            gatewayKeyPair.private,
            ZonedDateTime.now().plusYears(ENDPOINT_CERTIFICATE_VALIDITY_YEARS),
            gatewayCertificate
        )
        val registration = ClientRegistration(endpointCertificate, gatewayCertificate)
        return registration.serialize()
    }

    companion object {
        private const val AUTHORIZATION_VALIDITY_SECONDS: Long = 10
        private const val ENDPOINT_CERTIFICATE_VALIDITY_YEARS = 3L
    }
}
