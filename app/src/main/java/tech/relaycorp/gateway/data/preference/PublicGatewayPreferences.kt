package tech.relaycorp.gateway.data.preference

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.R
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Provider

class PublicGatewayPreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>,
    private val resources: Resources
) {

    // Address

    private val address by lazy {
        preferences.get().getString("address", DEFAULT_ADDRESS)
    }

    fun getAddress() = { address }.toFlow()
    suspend fun setAddress(value: String) = address.setAndCommit(value)

    // Certificate

    private val certificate by lazy {
        preferences.get().getString("certificate")
    }

    fun getCertificate() = { certificate }.toFlow()
        .map {
            val certificateBytes = if (it.isEmpty()) {
                resources.openRawResource(R.raw.public_gateway_cert).readBytesAndClose()
            } else {
                it.toByteArray(Charset.defaultCharset())
            }
            Certificate.deserialize(certificateBytes)
        }

    suspend fun setCertificate(value: Certificate) =
        address.setAndCommit(value.serialize().toString(Charset.defaultCharset()))

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_ADDRESS = "https://cogrpc-test.relaycorp.tech"
    }
}
