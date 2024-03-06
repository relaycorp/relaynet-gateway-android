package tech.relaycorp.gateway.data.preference

import android.util.Base64
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.data.disk.ReadRawFile
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.relaynet.NodeConnectionParams
import tech.relaycorp.relaynet.wrappers.deserializeRSAPublicKey
import tech.relaycorp.relaynet.wrappers.nodeId
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class InternetGatewayPreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>,
    private val readRawFile: ReadRawFile,
    private val resolveServiceAddress: ResolveServiceAddress,
) {
    // Address

    private val address by lazy {
        preferences.get().getString("address")
    }

    suspend fun getAddress(): String = observeAddress().first()

    fun observeAddress(): Flow<String> = { address }.toFlow()
        .map { it.ifEmpty { getDefaultParams().internetAddress } }

    suspend fun setAddress(value: String) = address.setAndCommit(value)

    @Throws(InternetAddressResolutionException::class)
    suspend fun getPoWebAddress() = resolveServiceAddress.resolvePoWeb(getAddress())

    // Public Key

    private val publicKey by lazy {
        preferences.get().getString("public_gateway_public_key")
    }

    suspend fun getPublicKey(): PublicKey = observePublicKey().first()

    private fun observePublicKey(): Flow<PublicKey> = { publicKey }.toFlow()
        .map {
            if (it.isEmpty()) {
                getDefaultParams().identityKey
            } else {
                Base64.decode(it, Base64.DEFAULT)
                    .deserializeRSAPublicKey()
            }
        }

    suspend fun setPublicKey(value: PublicKey) {
        publicKey.setAndCommit(Base64.encodeToString(value.encoded, Base64.DEFAULT))
        setId(value.nodeId)
    }

    // Node Id

    private val id by lazy {
        preferences.get().getString("public_gateway_id")
    }

    suspend fun getId(): String = id.get().ifEmpty {
        getPublicKey().nodeId.also {
            setId(it)
        }
    }

    private suspend fun setId(value: String) {
        id.setAndCommit(value)
    }

    // Registration State

    private val registrationState by lazy {
        preferences.get().getEnum("registration_state", RegistrationState.ToDo)
    }

    suspend fun getRegistrationState() = observeRegistrationState().first()
    fun observeRegistrationState() = { registrationState }.toFlow()
    suspend fun setRegistrationState(value: RegistrationState) =
        registrationState.setAndCommit(value)

    // Default Internet Gateway parameters

    private var defaultParams: NodeConnectionParams? = null

    private suspend fun getDefaultParams(): NodeConnectionParams = defaultParams ?: run {
        readRawFile.read(R.raw.public_gateway_cert)
            .let(NodeConnectionParams.Companion::deserialize)
            .also { defaultParams = it }
    }
}
