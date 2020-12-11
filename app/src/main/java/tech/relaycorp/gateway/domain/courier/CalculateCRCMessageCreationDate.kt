package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.domain.LocalConfig
import java.time.ZonedDateTime
import java.util.Collections.max
import javax.inject.Inject

class CalculateCRCMessageCreationDate
@Inject constructor(
    private val localConfig: LocalConfig
) {
    suspend fun calculate(): ZonedDateTime =
        max(
            listOf(
                nowInUtc().minusMinutes(5), // Allow for some clock-drift
                localConfig.getCertificate().startDate // Never before the GW registration
            )
        )
}
