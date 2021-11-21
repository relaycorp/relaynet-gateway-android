package tech.relaycorp.gateway.domain.publicsync

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.ServerException
import tech.relaycorp.relaynet.keystores.SessionPublicKeyStore
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import java.util.logging.Level
import javax.inject.Inject

@JvmSuppressWildcards
class RegisterGateway
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig,
    private val poWebClientBuilder: PoWebClientBuilder,
    private val resolveServiceAddress: ResolveServiceAddress,
    private val publicKeyStore: SessionPublicKeyStore
) {

    suspend fun registerIfNeeded(): Result {
        if (publicGatewayPreferences.getRegistrationState() != RegistrationState.ToDo) {
            return Result.AlreadyRegistered
        }

        val address = publicGatewayPreferences.getAddress()
        val result = register(address)
        if (result is Result.Registered) {
            saveSuccessfulResult(address, result.pnr)
        }
        return result
    }

    suspend fun registerNewAddress(newAddress: String): Result {
        val result = register(newAddress)
        if (result is Result.Registered) {
            saveSuccessfulResult(newAddress, result.pnr)
        }
        return result
    }

    private suspend fun register(address: String): Result {
        return try {
            val poWebAddress = resolveServiceAddress.resolvePoWeb(address)
            val poWeb = poWebClientBuilder.build(poWebAddress)
            val keyPair = localConfig.getIdentityKeyPair()

            poWeb.use {
                val pnrr = poWeb.preRegisterNode(keyPair.certificate.subjectPublicKey)
                val pnr = poWeb.registerNode(pnrr.serialize(keyPair.privateKey))

                if (pnr.gatewaySessionKey == null) {
                    logger.warning("Registration is missing public gateway's session key")
                    return Result.FailedToRegister
                }

                logger.info("Successfully registered with $address")
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

    private suspend fun saveSuccessfulResult(
        publicGatewayPublicAddress: String,
        registration: PrivateNodeRegistration
    ) {
        publicGatewayPreferences.setRegistrationState(RegistrationState.ToDo)
        publicGatewayPreferences.setAddress(publicGatewayPublicAddress)
        publicGatewayPreferences.setCertificate(registration.gatewayCertificate)
        localConfig.setIdentityCertificate(registration.privateNodeCertificate)
        publicKeyStore.save(
            registration.gatewaySessionKey!!,
            registration.gatewayCertificate.subjectPrivateAddress
        )
        publicGatewayPreferences.setRegistrationState(RegistrationState.Done)
    }

    sealed class Result {
        object FailedToResolve : Result()
        object FailedToRegister : Result()
        data class Registered(val pnr: PrivateNodeRegistration) : Result()
        object AlreadyRegistered : Result()
    }
}
