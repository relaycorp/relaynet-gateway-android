package tech.relaycorp.gateway

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import tech.relaycorp.gateway.test.AppTestProvider.app
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.Instant
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class AppTest {
    @Test
    fun mode() {
        assertEquals(App.Mode.Test, app.mode)
    }

    @Test
    fun instant() {
        assertNotNull(Instant.now())
    }

    @Test
    fun desugaringDependency() {
        val keyPair = generateRSAKeyPair()
        issueGatewayCertificate(keyPair.public, keyPair.private, ZonedDateTime.now().plusYears(1))
    }
}
