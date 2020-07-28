package tech.relaycorp.gateway.test

import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.issueParcelDeliveryAuthorization
import java.time.ZonedDateTime

/**
 * Full Relaynet PKI certification path from a public gateway to a PDA grantee.
 *
 * See also [CargoDeliveryCertPath]
 */
object FullCertPath {
    private val startDate by lazy { ZonedDateTime.now().minusMinutes(1) }
    private val endDate by lazy { ZonedDateTime.now().plusHours(1) }

    val PUBLIC_GW by lazy {
        issueGatewayCertificate(
            KeyPairSet.PUBLIC_GW.public,
            KeyPairSet.PUBLIC_GW.private,
            endDate,
            validityStartDate = startDate
        )
    }

    val PRIVATE_GW by lazy {
        issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.PUBLIC_GW.private,
            endDate,
            PUBLIC_GW,
            validityStartDate = startDate
        )
    }

    val PRIVATE_ENDPOINT by lazy {
        issueEndpointCertificate(
            KeyPairSet.PRIVATE_ENDPOINT.public,
            KeyPairSet.PRIVATE_GW.private,
            endDate,
            PRIVATE_GW,
            validityStartDate = startDate
        )
    }

    val PDA by lazy {
        issueParcelDeliveryAuthorization(
            KeyPairSet.PDA_GRANTEE.public,
            KeyPairSet.PRIVATE_ENDPOINT.private,
            endDate,
            PRIVATE_ENDPOINT,
            validityStartDate = startDate
        )
    }
}
