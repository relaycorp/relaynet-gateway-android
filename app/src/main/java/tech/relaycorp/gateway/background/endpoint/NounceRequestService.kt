package tech.relaycorp.gateway.background.endpoint

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import tech.relaycorp.gateway.background.component
import tech.relaycorp.gateway.domain.endpoint.GenerateEndpointNounce
import javax.inject.Inject

class NounceRequestService : Service() {

    @Inject
    lateinit var generateEndpointNounce: GenerateEndpointNounce

    override fun onBind(intent: Intent): IBinder? {
        component.inject(this)
        val messenger = Messenger(
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        NOUNCE_REQUEST -> reply(msg)
                    }
                }
            }
        )
        return messenger.binder
    }

    internal fun reply(requestMessage: Message) {
        val nounce = generateEndpointNounce.generate()
        val replyMessage = Message.obtain(null, NOUNCE_RESULT, nounce)
        requestMessage.replyTo.send(replyMessage)
    }

    companion object {
        const val NOUNCE_REQUEST = 1
        const val NOUNCE_RESULT = 2
    }
}
