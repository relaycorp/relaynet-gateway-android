package tech.relaycorp.gateway.data.preference

import androidx.annotation.VisibleForTesting
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.data.disk.ReadRawFile
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Provider

class PublicGatewayPreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>,
    private val readRawFile: ReadRawFile
) {

    // Address

    private val address by lazy {
        preferences.get().getString("address", DEFAULT_ADDRESS)
    }

    suspend fun getAddress() = observeAddress().first()
    private fun observeAddress() = { address }.toFlow()
    suspend fun setAddress(value: String) = address.setAndCommit(value)

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
                it.toByteArray(Charset.defaultCharset())
            }
            Certificate.deserialize(certificateBytes)
        }

    suspend fun setCertificate(value: Certificate) =
        certificate.setAndCommit(value.serialize().toString(Charset.defaultCharset()))

    // Registration State

    private val registrationState by lazy {
        preferences.get().getEnum("registration_state", RegistrationState.ToDo)
    }

    suspend fun getRegistrationState() = observeRegistrationState().first()
    private fun observeRegistrationState() = { registrationState }.toFlow()
    suspend fun setRegistrationState(value: RegistrationState) =
        registrationState.setAndCommit(value)

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_ADDRESS = "https://poweb-test.relaycorp.tech"
    }
}
