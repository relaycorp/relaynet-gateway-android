package tech.relaycorp.gateway.domain.publicsync

import tech.relaycorp.gateway.common.Logging.logger
import javax.inject.Inject

class PublicSync
@Inject constructor() {

    // TODO: implement me
    suspend fun sync() {
        logger.info("Public Sync")
        // deliver parcels
        // receive parcels
    }
}
