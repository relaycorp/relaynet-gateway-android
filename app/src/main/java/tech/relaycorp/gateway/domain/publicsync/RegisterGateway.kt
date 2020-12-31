package tech.relaycorp.gateway.domain.publicsync

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.PublicAddressResolutionException
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

    suspend fun registerIfNeeded(): Result {
        if (publicGatewayPreferences.getRegistrationState() != RegistrationState.ToDo) {
            return Result.AlreadyRegistered
        }

        val result = register()
        if (result is Result.Registered) {
            localConfig.setCertificate(result.pnr.privateNodeCertificate)
            publicGatewayPreferences.setCertificate(result.pnr.gatewayCertificate)
            publicGatewayPreferences.setRegistrationState(RegistrationState.Done)
        }
        return result
    }

    private suspend fun register(): Result {
        return try {
            val poWeb = poWebClientBuilder.build()
            val keyPair = localConfig.getKeyPair()

            poWeb.use {
                val pnrr = poWeb.preRegisterNode(keyPair.public)
                val pnr = poWeb.registerNode(pnrr.serialize(keyPair.private))
                Result.Registered(pnr)
            }
        } catch (e: ServerException) {
            logger.log(Level.INFO, "Could not register gateway due to server error", e)
            Result.FailedToRegister
        } catch (e: ClientBindingException) {
            logger.log(Level.SEVERE, "Could not register gateway due to client error", e)
            Result.FailedToRegister
        } catch (e: PublicAddressResolutionException) {
            logger.log(
                Level.WARNING,
                "Could not register gateway due to failure to resolve PoWeb address",
                e
            )
            Result.FailedToResolve
        }
    }

    sealed class Result {
        object FailedToResolve : Result()
        object FailedToRegister : Result()
        data class Registered(val pnr: PrivateNodeRegistration) : Result()
        object AlreadyRegistered : Result()
    }
}
