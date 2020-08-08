package tech.relaycorp.gateway.background.endpoint

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class NonceRequestServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun requestAndReceiveNonce() {
        val serviceIntent = Intent(
            getApplicationContext<Context>(),
            NonceRequestService::class.java
        )
        val binder = serviceRule.bindService(serviceIntent)

        var resultMessage: Message? = null

        val messenger = Messenger(binder)
        val requestMessage = Message.obtain(null, NonceRequestService.NONCE_REQUEST)
        requestMessage.replyTo = Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                resultMessage = Message.obtain().also { it.copyFrom(msg) }
            }
        })
        messenger.send(requestMessage)

        Thread.sleep(100)

        assertEquals(
            NonceRequestService.NONCE_RESULT,
            resultMessage?.what
        )
        assertNotNull(resultMessage?.obj)
    }

    @Test
    fun invalidRequestIsIgnored() {
        val serviceIntent = Intent(
            getApplicationContext<Context>(),
            NonceRequestService::class.java
        )
        val binder = serviceRule.bindService(serviceIntent)

        val messenger = Messenger(binder)
        val invalidMessage = Message.obtain(null, 999)

        messenger.send(invalidMessage)
    }
}
