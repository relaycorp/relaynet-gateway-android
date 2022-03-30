package tech.relaycorp.gateway.data.preference

import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.data.disk.ReadRawFile
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.relaynet.wrappers.deserializeRSAPublicKey
import tech.relaycorp.relaynet.wrappers.privateAddress
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PublicGatewayPreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>,
    private val readRawFile: ReadRawFile,
    private val resolveServiceAddress: ResolveServiceAddress
) {
    // Address

    private val address by lazy {
        preferences.get().getString("address", DEFAULT_ADDRESS)
    }

    suspend fun getAddress(): String = address.get()
    fun observeAddress(): Flow<String> = { address }.toFlow()
    suspend fun setAddress(value: String) = address.setAndCommit(value)

    suspend fun getCogRPCAddress() = "https://${getAddress()}"

    @Throws(PublicAddressResolutionException::class)
    suspend fun getPoWebAddress() = resolveServiceAddress.resolvePoWeb(getAddress())

    // Public Key

    private val publicKey by lazy {
        preferences.get().getString("public_gateway_public_key")
    }

    suspend fun getPublicKey(): PublicKey = observePublicKey().first()

    private fun observePublicKey(): Flow<PublicKey> = { publicKey }.toFlow()
        .map {
            if (it.isEmpty()) {
                readRawFile.read(R.raw.public_gateway_cert)
                    .let(Certificate.Companion::deserialize)
                    .subjectPublicKey
            } else {
                Base64.decode(it, Base64.DEFAULT)
                    .deserializeRSAPublicKey()
            }
        }

    suspend fun setPublicKey(value: PublicKey) {
        publicKey.setAndCommit(Base64.encodeToString(value.encoded, Base64.DEFAULT))
        setPrivateAddress(value.privateAddress)
    }

    // Private Address

    private val privateAddress by lazy {
        preferences.get().getString("public_gateway_private_address")
    }

    suspend fun getPrivateAddress(): String =
        privateAddress.get().ifEmpty {
            getPublicKey().privateAddress.also {
                setPrivateAddress(it)
            }
        }

    private suspend fun setPrivateAddress(value: String) {
        privateAddress.setAndCommit(value)
    }

    // Registration State

    private val registrationState by lazy {
        preferences.get().getEnum("registration_state", RegistrationState.ToDo)
    }

    suspend fun getRegistrationState() = observeRegistrationState().first()
    fun observeRegistrationState() = { registrationState }.toFlow()
    suspend fun setRegistrationState(value: RegistrationState) =
        registrationState.setAndCommit(value)

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_ADDRESS = "frankfurt.relaycorp.cloud"
    }
}
