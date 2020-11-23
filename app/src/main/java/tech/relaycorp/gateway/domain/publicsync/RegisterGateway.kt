package tech.relaycorp.gateway.domain.publicsync

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.ServerException
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import java.util.logging.Level
import javax.inject.Inject

@JvmSuppressWildcards
class RegisterGateway
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig,
    private val poWebClientBuilder: PoWebClientBuilder
) {

    suspend fun registerIfNeeded() {
        if (publicGatewayPreferences.getRegistrationState() != RegistrationState.ToDo) {
            return
        }

        val pnr = register() ?: return
        localConfig.setCertificate(pnr.privateNodeCertificate)
        publicGatewayPreferences.setCertificate(pnr.gatewayCertificate)
        publicGatewayPreferences.setRegistrationState(RegistrationState.Done)
    }

    private suspend fun register(): PrivateNodeRegistration? =
        try {
            val poWeb = poWebClientBuilder.build()
            val keyPair = localConfig.getKeyPair()

            poWeb.use {
                val pnrr = poWeb.preRegisterNode(keyPair.public)
                poWeb.registerNode(pnrr.serialize(keyPair.private))
            }
        } catch (e: ServerException) {
            logger.log(Level.INFO, "Could not register gateway due to server error", e)
            null
        } catch (e: ClientBindingException) {
            logger.log(Level.SEVERE, "Could not register gateway due to client error", e)
            null
        }
}
