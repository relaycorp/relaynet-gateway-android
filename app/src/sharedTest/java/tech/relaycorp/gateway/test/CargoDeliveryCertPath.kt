package tech.relaycorp.gateway.test

import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import java.time.ZonedDateTime

/**
 * Awala PKI certification path from a private gateway to a public gateway.
 */
object CargoDeliveryCertPath {
    private val startDate by lazy { ZonedDateTime.now().minusMinutes(1) }
    private val endDate by lazy { ZonedDateTime.now().plusHours(1) }

    val PRIVATE_GW by lazy {
        issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.PRIVATE_GW.private,
            endDate,
            validityStartDate = startDate
        )
    }

    // TODO: Replace issueGatewayCertificate() with issueCargoDeliveryAuthorization() once it's
    // implemented: https://github.com/relaycorp/relaynet-jvm/issues/76
    val PUBLIC_GW by lazy {
        issueGatewayCertificate(
            KeyPairSet.INTERNET_GW.public,
            KeyPairSet.PRIVATE_GW.private,
            endDate,
            PRIVATE_GW,
            validityStartDate = startDate
        )
    }
}
