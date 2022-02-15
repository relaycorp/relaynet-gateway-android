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
            PrivateNodeRegistrationAuthorization(expiryDate, endpointApplicationId.toByteArray())
        return authorization.serialize(localConfig.getIdentityKey())
    }

    /**
     * Complete endpoint registration and return registration serialized.
     */
    @Throws(InvalidPNRAException::class)
    suspend fun register(request: PrivateNodeRegistrationRequest): ByteArray {
        val identityKey = localConfig.getIdentityKey()
        val identityCert = localConfig.getIdentityCertificate()
        val authorization = try {
            PrivateNodeRegistrationAuthorization.deserialize(
                request.pnraSerialized,
                identityCert.subjectPublicKey
            )
        } catch (exc: InvalidMessageException) {
            throw InvalidPNRAException("Registration request contains invalid authorization", exc)
        }
        val applicationId = authorization.gatewayData.toString(Charset.defaultCharset())
        val endpoint = LocalEndpoint(
            MessageAddress.of(request.privateNodePublicKey.privateAddress),
            applicationId
        )
        localEndpointDao.insert(endpoint)

        val endpointCertificate = issueEndpointCertificate(
            request.privateNodePublicKey,
            identityKey,
            ZonedDateTime.now().plusYears(ENDPOINT_CERTIFICATE_VALIDITY_YEARS),
            identityCert
        )
        val registration = PrivateNodeRegistration(endpointCertificate, identityCert)
        return registration.serialize()
    }

    companion object {
        private const val AUTHORIZATION_VALIDITY_SECONDS: Long = 15
        private const val ENDPOINT_CERTIFICATE_VALIDITY_YEARS = 3L
    }
}
