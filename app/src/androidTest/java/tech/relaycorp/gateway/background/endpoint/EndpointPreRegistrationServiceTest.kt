package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.AppTestProvider
import tech.relaycorp.gateway.test.KeystoreResetTestRule
import tech.relaycorp.gateway.test.WaitAssertions.waitFor
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.keystores.CertificateStore
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationAuthorization
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.nodeId
import java.nio.charset.Charset
import java.time.ZonedDateTime
import javax.inject.Inject

class EndpointPreRegistrationServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @get:Rule
    val keystoreResetRule = KeystoreResetTestRule()

    @Inject
    lateinit var app: App

    @Inject
    lateinit var localConfig: LocalConfig

    @Inject
    lateinit var certificateStore: CertificateStore

    @Inject
    lateinit var internetGatewayPreferences: InternetGatewayPreferences

    private val coroutineContext
        get() = (getApplicationContext() as App).backgroundContext

    @Before
    fun setUp() {
        AppTestProvider.component.inject(this)
        runTest(coroutineContext) {
            internetGatewayPreferences.setRegistrationState(RegistrationState.Done)
            certificateStore.delete(
                localConfig.getIdentityKey().nodeId,
                internetGatewayPreferences.getId(),
            )
        }
    }

    @Test
    fun requestPreRegistration() = runTest(coroutineContext) {
        setIdentityCertificate(ZonedDateTime.now().plusMinutes(1))

        val serviceIntent = Intent(
            getApplicationContext(),
            EndpointPreRegistrationService::class.java,
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
            resultMessage!!.what,
        )

        // Check we got a valid authorization
        val resultData = resultMessage!!.data
        assertTrue(resultData.containsKey("auth"))
        val gatewayCert = localConfig.getParcelDeliveryCertificate()!!
        val authorization = PrivateNodeRegistrationAuthorization.deserialize(
            resultData.getByteArray("auth")!!,
            gatewayCert.subjectPublicKey,
        )
        assertEquals(
            getApplicationContext<Context>().packageName,
            authorization.gatewayData.toString(Charset.defaultCharset()),
        )
    }

    @Test
    fun invalidRequestIsIgnored() {
        val serviceIntent = Intent(
            getApplicationContext(),
            EndpointPreRegistrationService::class.java,
        )
        val binder = serviceRule.bindService(serviceIntent)

        val messenger = Messenger(binder)
        val invalidMessage = Message.obtain(null, 999)

        messenger.send(invalidMessage)
    }

    @Test
    fun errorReturnedWhenGatewayIsNotRegisteredYet() = runTest(coroutineContext) {
        internetGatewayPreferences.setRegistrationState(RegistrationState.ToDo)

        val serviceIntent = Intent(
            getApplicationContext(),
            EndpointPreRegistrationService::class.java,
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
            resultMessage!!.what,
        )
    }

    @Test
    fun errorReturnedWhenGatewaysCertificateIsExpired() = runTest(coroutineContext) {
        val serviceIntent = Intent(
            getApplicationContext(),
            EndpointPreRegistrationService::class.java,
        )
        val binder = serviceRule.bindService(serviceIntent)

        // Deleting all our certificates it's the same as having only expired certificates
        certificateStore.delete(
            localConfig.getIdentityKey().nodeId,
            internetGatewayPreferences.getId(),
        )

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
            resultMessage!!.what,
        )
    }

    private suspend fun setIdentityCertificate(validityEndDate: ZonedDateTime) {
        val expiredCertificate = issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.INTERNET_GW.private,
            validityEndDate = validityEndDate,
            PDACertPath.INTERNET_GW,
            validityStartDate = ZonedDateTime.now().minusMinutes(1),
        )
        localConfig.setParcelDeliveryCertificate(expiredCertificate)
    }
}
