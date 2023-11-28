package tech.relaycorp.gateway.data.doh

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException
import tech.relaycorp.gateway.data.model.ServiceAddress
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ResolveServiceAddress
@Inject constructor(
    private val doHClient: DoHClient,
) {
    @Throws(InternetAddressResolutionException::class)
    suspend fun resolvePoWeb(address: String): ServiceAddress {
        val srvRecordName = "_awala-gsc._tcp.$address"
        val answer = try {
            withTimeout(5.seconds) {
                doHClient.lookUp(srvRecordName, "SRV")
            }
        } catch (exc: LookupFailureException) {
            throw InternetAddressResolutionException(
                "Failed to resolve DNS for PoWeb address",
                exc,
            )
        } catch (exc: TimeoutCancellationException) {
            throw InternetAddressResolutionException(
                "Failed to resolve DNS for PoWeb address",
                exc,
            )
        }
        val srvRecordData = answer.data.first()
        val srvRecordDataFields = srvRecordData.split(" ")
        if (srvRecordDataFields.size < 4) {
            throw InternetAddressResolutionException(
                "Malformed SRV for $address ($srvRecordData)",
            )
        }
        val targetHost = srvRecordDataFields[3]
        val targetPort = srvRecordDataFields[2]
        return ServiceAddress(targetHost.trimEnd('.'), targetPort.toInt())
    }
}
