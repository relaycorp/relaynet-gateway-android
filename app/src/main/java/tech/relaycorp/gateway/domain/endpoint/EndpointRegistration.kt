package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationAuthorization
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import tech.relaycorp.relaynet.wrappers.nodeId
import java.nio.charset.Charset
import java.time.ZonedDateTime
import javax.inject.Inject

class EndpointRegistration
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao,
    private val localConfig: LocalConfig,
) {
    /**
     * Issue PNRA for an application to register one or more of its endpoints.
     */
    suspend fun authorize(endpointApplicationId: String): ByteArray {
        val expiryDate = ZonedDateTime.now().plusSeconds(AUTHORIZATION_VALIDITY_SECONDS)
        val authorization = PrivateNodeRegistrationAuthorization(
            expiryDate,
            endpointApplicationId.toByteArray(),
        )
        return authorization.serialize(localConfig.getIdentityKey())
    }

    /**
     * Complete endpoint registration and return registration serialized.
     */
    @Throws(InvalidPNRAException::class, GatewayNotRegisteredException::class)
    suspend fun register(request: PrivateNodeRegistrationRequest): ByteArray {
        val identityKey = localConfig.getIdentityKey()
        val identityCert = localConfig.getParcelDeliveryCertificate()
            ?: throw GatewayNotRegisteredException()
        val authorization = try {
            PrivateNodeRegistrationAuthorization.deserialize(
                request.pnraSerialized,
                identityCert.subjectPublicKey,
            )
        } catch (exc: InvalidMessageException) {
            throw InvalidPNRAException(
                "Registration request contains invalid authorization",
                exc,
            )
        }
        val applicationId = authorization.gatewayData.toString(Charset.defaultCharset())
        val endpoint = LocalEndpoint(
            MessageAddress.of(request.privateNodePublicKey.nodeId),
            applicationId,
        )
        localEndpointDao.insert(endpoint)

        val endpointCertificate = issueEndpointCertificate(
            request.privateNodePublicKey,
            identityKey,
            identityCert.expiryDate,
            identityCert,
        )
        val registration = PrivateNodeRegistration(
            endpointCertificate,
            identityCert,
            localConfig.getInternetGatewayAddress(),
        )
        return registration.serialize()
    }

    companion object {
        private const val AUTHORIZATION_VALIDITY_SECONDS: Long = 15
    }
}
