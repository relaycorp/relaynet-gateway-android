package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import com.schibsted.spain.barista.rule.cleardata.ClearPreferencesRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.gateway.test.WaitAssertions.waitFor
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationAuthorization
import java.nio.charset.Charset
import javax.inject.Inject

class EndpointPreRegistrationServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()
    @get:Rule
    val clearPreferencesRule = ClearPreferencesRule()

    @Inject
    lateinit var localConfig: LocalConfig

    @Inject
    lateinit var publicGatewayPreferences: PublicGatewayPreferences

    @Before
    fun setUp() {
        AppTestProvider.component.inject(this)
        runBlocking {
            localConfig.generateKeyPair()
            publicGatewayPreferences.setRegistrationState(RegistrationState.Done)
        }
    }

    @After
    fun tearDown() {
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
            Message.obtain(handler, EndpointPreRegistrationService.PRE_REGISTRATION_REQUEST)
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
        val resultData = resultMessage!!.data
        assertTrue(resultData.containsKey("auth"))
        val gatewayKeyPair = localConfig.getKeyPair()
        val authorization = PrivateNodeRegistrationAuthorization.deserialize(
            resultData.getByteArray("auth")!!,
            gatewayKeyPair.public
        )
        assertEquals(
            getApplicationContext<Context>().packageName,
            authorization.gatewayData.toString(Charset.defaultCharset())
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

    @Test
    fun errorReturnedWhenGatewayIsNotRegisteredYet() {
        runBlocking {
            publicGatewayPreferences.setRegistrationState(RegistrationState.ToDo)
        }

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
            Message.obtain(handler, EndpointPreRegistrationService.PRE_REGISTRATION_REQUEST)
        requestMessage.replyTo = Messenger(handler)
        messenger.send(requestMessage)

        waitFor {
            assertNotNull("We should have got a reply", resultMessage)
        }
        assertEquals(
            EndpointPreRegistrationService.GATEWAY_NOT_REGISTERED,
            resultMessage!!.what
        )
    }
}
