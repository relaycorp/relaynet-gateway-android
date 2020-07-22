package tech.relaycorp.gateway.background.endpoint

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import tech.relaycorp.gateway.background.component
import tech.relaycorp.gateway.domain.endpoint.GenerateEndpointNonce
import javax.inject.Inject

class NonceRequestService : Service() {

    @Inject
    lateinit var generateEndpointNonce: GenerateEndpointNonce

    override fun onBind(intent: Intent): IBinder? {
        component.inject(this)
        val messenger = Messenger(
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        NONCE_REQUEST -> reply(msg)
                    }
                }
            }
        )
        return messenger.binder
    }

    internal fun reply(requestMessage: Message) {
        val nonce = generateEndpointNonce.generate()
        val replyMessage = Message.obtain(null, NONCE_RESULT, nonce)
        requestMessage.replyTo.send(replyMessage)
    }

    companion object {
        const val NONCE_REQUEST = 1
        const val NONCE_RESULT = 2
    }
}
