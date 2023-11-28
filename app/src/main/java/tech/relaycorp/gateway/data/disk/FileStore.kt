package tech.relaycorp.gateway.data.disk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.common.Logging.logger
import java.io.File
import java.io.FileNotFoundException
import java.util.logging.Level
import javax.inject.Inject

class FileStore
@Inject constructor(
    private val context: Context,
) {

    suspend fun store(location: String, data: ByteArray) {
        withContext(Dispatchers.IO) {
            delete(location)
            buildFile(location).writeBytes(data)
        }
    }

    suspend fun read(location: String) = withContext(Dispatchers.IO) {
        try {
            buildFile(location).readBytes()
        } catch (exception: FileNotFoundException) {
            logger.log(Level.INFO, "File $location does not exist")
            null
        }
    }

    suspend fun delete(location: String) {
        withContext(Dispatchers.IO) {
            buildFile(location).delete()
        }
    }

    private fun buildFile(location: String) = File(context.filesDir, location)
}
