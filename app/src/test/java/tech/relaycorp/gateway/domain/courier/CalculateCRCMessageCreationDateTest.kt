package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair

class CalculateCRCMessageCreationDateTest {

    private val localConfig = mock<LocalConfig>()
    private val subject = CalculateCRCMessageCreationDate(localConfig)

    @Test
    internal fun `creation date 90 minutes past if registration was before`() = runBlockingTest {
        val keyPair = generateRSAKeyPair()
        val certificate = issueGatewayCertificate(
            keyPair.public,
            keyPair.private,
            nowInUtc().plusMinutes(1),
            validityStartDate = nowInUtc().minusDays(1)
        )
        whenever(localConfig.getCertificate()).thenReturn(certificate)

        val result = subject.calculate()

        assertTrue(
            result.isBefore(nowInUtc().minusMinutes(89)) &&
                result.isAfter(nowInUtc().minusMinutes(91))
        )
    }

    @Test
    internal fun `creation date equal to registration if sooner than 90 minutes`() =
        runBlockingTest {
            val keyPair = generateRSAKeyPair()
            val certificate = issueGatewayCertificate(
                keyPair.public,
                keyPair.private,
                nowInUtc().plusMinutes(1),
                validityStartDate = nowInUtc()
            )
            whenever(localConfig.getCertificate()).thenReturn(certificate)

            val result = subject.calculate()
            assertTrue(certificate.startDate.isEqual(result))
        }
}
