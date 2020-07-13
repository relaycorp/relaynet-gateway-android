package tech.relaycorp.gateway.data.preference

import androidx.annotation.VisibleForTesting
import com.tfcporciuncula.flow.FlowSharedPreferences
import javax.inject.Inject
import javax.inject.Provider

class PublicGatewayPreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>
) {

    private val address by lazy {
        preferences.get().getString("address", DEFAULT_ADDRESS)
    }

    fun getAddress() = { address }.toFlow()
    suspend fun getAddress(value: String) = address.setAndCommit(value)

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_ADDRESS = "https://cogrpc-test.relaycorp.tech"
    }
}
