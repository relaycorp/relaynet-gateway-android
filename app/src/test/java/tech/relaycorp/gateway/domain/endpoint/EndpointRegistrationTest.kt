package tech.relaycorp.gateway.domain.endpoint

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.relaynet.messages.control.ClientRegistrationAuthorization
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndpointRegistrationTest {
    private val mockLocalEndpointDao = mock<LocalEndpointDao>()
    private val mockLocalConfig = mock<LocalConfig>()
    private val endpointRegistration = EndpointRegistration(mockLocalEndpointDao, mockLocalConfig)

    private val dummyApplicationId = "tech.relaycorp.foo"

    @Nested
    inner class Authorize {
        @BeforeEach
        internal fun setUp() = runBlockingTest {
            whenever(mockLocalConfig.getKeyPair()).thenReturn(KeyPairSet.PRIVATE_GW)
        }

        @Test
        fun `Application Id for endpoint should be stored in server data`() = runBlockingTest {
            val craSerialized = endpointRegistration.authorize(dummyApplicationId)

            val cra = ClientRegistrationAuthorization.deserialize(
                craSerialized,
                KeyPairSet.PRIVATE_GW.public
            )

            assertEquals(dummyApplicationId, cra.serverData.toString(Charset.defaultCharset()))
        }

        @Test
        fun `Authorization should be valid for 5 seconds`() = runBlockingTest {
            val craSerialized = endpointRegistration.authorize(dummyApplicationId)

            val cra = ClientRegistrationAuthorization.deserialize(
                craSerialized,
                KeyPairSet.PRIVATE_GW.public
            )

            val craTTL = ChronoUnit.SECONDS.between(ZonedDateTime.now(), cra.expiryDate)
            assertTrue(craTTL in 3..5) // Give some wiggle room
        }
    }
}
