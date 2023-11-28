package tech.relaycorp.gateway.background.publicsync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.domain.publicsync.PublicSync
import java.util.concurrent.CancellationException
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Provider

class PublicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val publicSync: PublicSync,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            publicSync.syncOneOff()
            Result.success()
        } catch (exception: CancellationException) {
            logger.log(Level.INFO, "Cancellation", exception)
            Result.failure()
        } catch (exception: Exception) {
            logger.log(Level.SEVERE, "Unexpected issue", exception)
            Result.failure()
        }
    }
}

class PublicSyncWorkerFactory
@Inject constructor(
    private val PublicSync: Provider<PublicSync>,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ) = PublicSyncWorker(appContext, workerParameters, PublicSync.get())
}
