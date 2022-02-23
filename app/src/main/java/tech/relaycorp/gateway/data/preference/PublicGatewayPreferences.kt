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
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import javax.inject.Inject
import javax.inject.Provider

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

    // Certificate

    private val certificate by lazy {
        preferences.get().getString("public_gateway_certificate")
    }

    suspend fun getCertificate() = observeCertificate().first()

    private fun observeCertificate() = { certificate }.toFlow()
        .map {
            val certificateBytes = if (it.isEmpty()) {
                readRawFile.read(R.raw.public_gateway_cert)
            } else {
                Base64.decode(it, Base64.DEFAULT)
            }
            Certificate.deserialize(certificateBytes)
        }

    suspend fun setCertificate(value: Certificate) {
        certificate.setAndCommit(Base64.encodeToString(value.serialize(), Base64.DEFAULT))
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
