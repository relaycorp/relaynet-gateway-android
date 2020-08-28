package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.gateway.test.WaitAssertions.waitFor
import tech.relaycorp.relaynet.messages.control.ClientRegistrationAuthorization
import java.nio.charset.Charset
import javax.inject.Inject

class EndpointPreRegistrationServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Inject
    lateinit var localConfig: LocalConfig

    @Before
    fun setUp() {
        AppTestProvider.component.inject(this)
    }

    @Test
    fun requestPreRegistration() = runBlocking {
        val serviceIntent = Intent(
            getApplicationContext<Context>(),
            EndpointPreRegistrationService::class.java
        )
        val binder = serviceRule.bindService(serviceIntent)

        var resultMessage: Message? = null

        val messenger = Messenger(binder)
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                resultMessage = Message.obtain().also { it.copyFrom(msg) }
            }
        }
        val requestMessage =
            Message.obtain(handler, EndpointPreRegistrationService.PREREGISTRATION_REQUEST)
        requestMessage.replyTo = Messenger(handler)
        messenger.send(requestMessage)

        waitFor {
            assertNotNull("We should have got a reply", resultMessage)
        }
        assertEquals(
            EndpointPreRegistrationService.REGISTRATION_AUTHORIZATION,
            resultMessage!!.what
        )

        // Check we got a valid authorization
        assertTrue(resultMessage!!.obj is ByteArray)
        val gatewayKeyPair = localConfig.getKeyPair()
        val authorization = ClientRegistrationAuthorization.deserialize(
            resultMessage!!.obj as ByteArray,
            gatewayKeyPair.public
        )
        assertEquals(
            getApplicationContext<Context>().packageName,
            authorization.serverData.toString(Charset.defaultCharset())
        )
    }

    @Test
    fun invalidRequestIsIgnored() {
        val serviceIntent = Intent(
            getApplicationContext<Context>(),
            EndpointPreRegistrationService::class.java
        )
        val binder = serviceRule.bindService(serviceIntent)

        val messenger = Messenger(binder)
        val invalidMessage = Message.obtain(null, 999)

        messenger.send(invalidMessage)
    }
}
