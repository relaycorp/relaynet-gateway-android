package tech.relaycorp.gateway.data.doh

import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException
import tech.relaycorp.gateway.data.model.ServiceAddress
import javax.inject.Inject

class ResolveServiceAddress
@Inject constructor(
    private val doHClient: DoHClient
) {
    @Throws(PublicAddressResolutionException::class)
    suspend fun resolvePoWeb(address: String): ServiceAddress {
            val srvRecordName = "_rgsc._tcp.$address"
            val answer = try {
                doHClient.lookUp(srvRecordName, "SRV")
            } catch (exc: LookupFailureException) {
                throw PublicAddressResolutionException(
                    "Failed to resolve DNS for PoWeb address",
                    exc
                )
            }
            val srvRecordData = answer.data.first()
            val srvRecordDataFields = srvRecordData.split(" ")
            if (srvRecordDataFields.size < 4) {
                throw PublicAddressResolutionException(
                    "Malformed SRV for $address ($srvRecordData)"
                )
            }
            val targetHost = srvRecordDataFields[3]
            val targetPort = srvRecordDataFields[2]
            return ServiceAddress(targetHost.trimEnd('.'), targetPort.toInt())
    }
}
