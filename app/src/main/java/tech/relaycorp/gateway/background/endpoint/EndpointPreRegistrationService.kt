package tech.relaycorp.gateway.background.endpoint

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import tech.relaycorp.gateway.background.component
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import javax.inject.Inject

class EndpointPreRegistrationService : Service() {
    private val scope get() = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var endpointRegistration: EndpointRegistration

    override fun onBind(intent: Intent): IBinder? {
        component.inject(this)
        val messenger = Messenger(
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        PREREGISTRATION_REQUEST -> reply(msg)
                    }
                }
            }
        )
        return messenger.binder
    }

    // TODO: Replace runBlocking with scope.launch
    internal fun reply(requestMessage: Message) = runBlocking {
        // TODO: Dynamically compute the application id
        val craSerialized = endpointRegistration.authorize("appId")
        val replyMessage = Message.obtain(null, REGISTRATION_AUTHORIZATION, craSerialized)
        requestMessage.replyTo.send(replyMessage)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val PREREGISTRATION_REQUEST = 1
        const val REGISTRATION_AUTHORIZATION = 2
    }
}
