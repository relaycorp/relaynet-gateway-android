package tech.relaycorp.gateway.data.disk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiskMessageOperations
@Inject constructor(
    private val context: Context,
) {

    @Throws(DiskException::class)
    suspend fun writeMessage(folder: String, prefix: String, message: ByteArray): String = try {
        writeMessageUnhandled(folder, prefix, message)
    } catch (e: IOException) {
        throw DiskException(e)
    }

    suspend fun listMessages(folder: String): List<() -> InputStream> =
        withContext(Dispatchers.IO) {
            getOrCreateDir(folder)
                .listFiles()
                ?.map { it::inputStream }
                ?: emptyList()
        }

    @Throws(MessageDataNotFoundException::class)
    suspend fun readMessage(folder: String, path: String): (() -> InputStream) {
        val file = File(getOrCreateDir(folder), path)
        if (!file.exists()) {
            throw MessageDataNotFoundException("Message data not found on path '$path'")
        }
        return file::inputStream
    }

    suspend fun deleteMessage(folder: String, path: String) {
        withContext(Dispatchers.IO) {
            val messagesDir = getOrCreateDir(folder)
            File(messagesDir, path).delete()
        }
    }

    suspend fun deleteAllMessages(folder: String) {
        withContext(Dispatchers.IO) {
            getOrCreateDir(folder).deleteRecursively()
        }
    }

    private suspend fun writeMessageUnhandled(folder: String, prefix: String, message: ByteArray) =
        withContext(Dispatchers.IO) {
            val file = createUniqueFile(folder, prefix)
            file.writeBytes(message)
            file.name
        }

    private suspend fun getOrCreateDir(folder: String) = withContext(Dispatchers.IO) {
        File(context.filesDir, folder).also {
            if (!it.exists()) it.mkdir()
        }
    }

    private suspend fun createUniqueFile(folder: String, prefix: String) =
        withContext(Dispatchers.IO) {
            val filesDir = getOrCreateDir(folder)
            // The file created isn't temporary, but it ensures a unique filename
            File.createTempFile(prefix, "", filesDir)
        }
}
