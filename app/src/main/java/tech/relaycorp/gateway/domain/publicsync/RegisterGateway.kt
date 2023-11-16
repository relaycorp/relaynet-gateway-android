package tech.relaycorp.gateway.domain.publicsync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.endpoint.GatewayCertificateChangeNotifier
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.ServerException
import tech.relaycorp.relaynet.keystores.SessionPublicKeyStore
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import java.time.Duration
import java.time.ZonedDateTime
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@JvmSuppressWildcards
class RegisterGateway
@Inject constructor(
    private val gatewayCertificateChangeNotifier: GatewayCertificateChangeNotifier,
    private val internetGatewayPreferences: InternetGatewayPreferences,
    private val localConfig: LocalConfig,
    private val poWebClientBuilder: PoWebClientBuilder,
    private val resolveServiceAddress: ResolveServiceAddress,
    private val publicKeyStore: SessionPublicKeyStore
) {

    private val mutex = Mutex()

    suspend fun registerIfNeeded(): Result {
        mutex.withLock {
            val isFirstRegistration =
                internetGatewayPreferences.getRegistrationState() == RegistrationState.ToDo

            if (
                !isFirstRegistration &&
                !currentCertificateIsAboutToExpire()
            ) {
                return Result.AlreadyRegisteredAndNotExpiring
            }

            val address = internetGatewayPreferences.getAddress()
            val result = register(address)
            if (result is Result.Registered) {
                saveSuccessfulResult(address, result.pnr)

                if (!isFirstRegistration) {
                    gatewayCertificateChangeNotifier.notifyAll()
                }
            }

            return result
        }
    }

    suspend fun registerNewAddress(newAddress: String): Result {
        mutex.withLock {
            val result = register(newAddress)
            if (result is Result.Registered) {
                saveSuccessfulResult(newAddress, result.pnr)
            }
            return result
        }
    }

    private suspend fun currentCertificateIsAboutToExpire() =
        localConfig.getIdentityCertificate().expiryDate < ZonedDateTime.now().plus(ABOUT_TO_EXPIRE)

    private suspend fun register(address: String): Result {
        return try {
            logger.info("Registering with $address")

            val poWebAddress = resolveServiceAddress.resolvePoWeb(address)
            val poWeb = poWebClientBuilder.build(poWebAddress)
            val privateKey = localConfig.getIdentityKey()
            val certificate = localConfig.getIdentityCertificate()

            poWeb.use {
                val pnrr = poWeb.preRegisterNode(certificate.subjectPublicKey)
                val pnr = poWeb.registerNode(pnrr.serialize(privateKey))

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
        } catch (e: InternetAddressResolutionException) {
            logger.log(
                Level.WARNING,
                "Could not register gateway due to failure to resolve PoWeb address",
                e
            )
            Result.FailedToResolve
        }
    }

    private suspend fun saveSuccessfulResult(
        internetGatewayAddress: String,
        registration: PrivateNodeRegistration
    ) {
        internetGatewayPreferences.setRegistrationState(RegistrationState.ToDo)
        internetGatewayPreferences.setAddress(internetGatewayAddress)
        internetGatewayPreferences.setPublicKey(registration.gatewayCertificate.subjectPublicKey)
        localConfig.setIdentityCertificate(registration.privateNodeCertificate)
        publicKeyStore.save(
            registration.gatewaySessionKey!!,
            registration.gatewayCertificate.subjectId
        )
        internetGatewayPreferences.setRegistrationState(RegistrationState.Done)
    }

    sealed class Result {
        data object FailedToResolve : Result()
        data object FailedToRegister : Result()
        data class Registered(val pnr: PrivateNodeRegistration) : Result()
        data object AlreadyRegisteredAndNotExpiring : Result()

        val isSuccessful
            get() = this is Registered || this is AlreadyRegisteredAndNotExpiring
    }

    companion object {
        private val ABOUT_TO_EXPIRE = Duration.ofDays(90)
    }
}
