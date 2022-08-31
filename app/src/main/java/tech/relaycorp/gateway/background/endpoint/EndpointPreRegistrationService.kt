package tech.relaycorp.gateway.background.endpoint

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.background.component
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import java.util.logging.Level
import javax.inject.Inject

class EndpointPreRegistrationService : Service() {
    private val scope get() = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var internetGatewayPreferences: InternetGatewayPreferences

    @Inject
    lateinit var endpointRegistration: EndpointRegistration

    override fun onBind(intent: Intent): IBinder? {
        component.inject(this)
        val messenger = Messenger(
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    onMessageReceived(msg)
                }
            }
        )
        return messenger.binder
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    internal fun onMessageReceived(message: Message) {
        // The original message will be recycled when the scope changes
        val localMessage = Message.obtain(message)
        scope.launch {
            when (localMessage.what) {
                PRE_REGISTRATION_REQUEST -> reply(localMessage)
            }
        }
    }

    private suspend fun reply(requestMessage: Message) {
        val endpointApplicationId = getApplicationNameForUID(requestMessage.sendingUid)
        val replyMessage = when {
            endpointApplicationId == null -> {
                logger.log(Level.WARNING, "Could not get applicationId from caller")
                Message.obtain(null, PRE_REGISTRATION_ERROR)
            }

            internetGatewayPreferences.getRegistrationState() != RegistrationState.Done -> {
                logger.log(Level.WARNING, "Gateway not ready for registration")
                Message.obtain(null, GATEWAY_NOT_REGISTERED)
            }

            else -> {
                val authorizationSerialized = endpointRegistration.authorize(endpointApplicationId)
                Message.obtain(null, REGISTRATION_AUTHORIZATION).also {
                    it.data = Bundle().apply { putByteArray("auth", authorizationSerialized) }
                }
            }
        }
        requestMessage.replyTo.send(replyMessage)
    }

    private fun getApplicationNameForUID(uid: Int) =
        applicationContext.packageManager.getNameForUid(uid)

    companion object {
        const val PRE_REGISTRATION_REQUEST = 1
        const val REGISTRATION_AUTHORIZATION = 2
        const val PRE_REGISTRATION_ERROR = 3
        const val GATEWAY_NOT_REGISTERED = 4
    }
}
