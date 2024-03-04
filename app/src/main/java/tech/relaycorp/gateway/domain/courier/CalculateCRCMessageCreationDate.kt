package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.domain.LocalConfig
import java.time.ZonedDateTime
import java.util.Collections.max
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class CalculateCRCMessageCreationDate
@Inject constructor(
    private val localConfig: LocalConfig,
) {
    suspend fun calculate(): ZonedDateTime = max(
        listOf(
            nowInUtc().minus(CLOCK_DRIFT_TOLERANCE.toJavaDuration()),
            // Never before the GW registration
            localConfig.getCargoDeliveryAuth().startDate,
        ),
    )

    companion object {
        val CLOCK_DRIFT_TOLERANCE = 90.minutes
    }
}
