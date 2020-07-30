package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueEndpointCertificate
import javax.inject.Inject

class RegisterEndpoint
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao,
    private val localConfig: LocalConfig
) {

    suspend fun register(applicationId: String, endpointAddress: MessageAddress) {
        localEndpointDao.insert(LocalEndpoint(endpointAddress, applicationId))
    }

    suspend fun getGatewayCertificate() = localConfig.getCertificate()

    suspend fun generateEndpointCertificate(endpointAddress: MessageAddress) {
        val gatewayKeyPair = localConfig.getKeyPair()
        issueEndpointCertificate(
            gatewayKeyPair.public,
            gatewayKeyPair.private,
            nowInUtc().plusYears(ENDPOINT_CERTIFICATE_VALIDITY_YEARS),
            getGatewayCertificate()
        )
    }

    companion object {
        private const val ENDPOINT_CERTIFICATE_VALIDITY_YEARS = 3L
    }
}
