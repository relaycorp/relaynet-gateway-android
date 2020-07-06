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
class DiskRepository
@Inject constructor(
    private val context: Context
) {

    @Throws(DiskException::class)
    suspend fun writeParcel(parcel: ByteArray): String =
        try {
            writeParcelUnhandled(parcel)
        } catch (e: IOException) {
            throw DiskException(e)
        }

    @Throws(ParcelDataNotFoundException::class)
    suspend fun readParcel(path: String): (() -> InputStream) {
        val file = File(getOrCreateParcelDir(), path)
        if (!file.exists()) throw ParcelDataNotFoundException("Parcel data not found on path '$path'")
        return file::inputStream
    }

    suspend fun deleteMessage(path: String) {
        withContext(Dispatchers.IO) {
            val messagesDir = getOrCreateParcelDir()
            File(messagesDir, path).delete()
        }
    }

    private suspend fun writeParcelUnhandled(parcel: ByteArray) =
        withContext(Dispatchers.IO) {
            val file = createUniqueFile()
            file.writeBytes(parcel)
            file.name
        }

    private suspend fun getOrCreateParcelDir() =
        withContext(Dispatchers.IO) {
            File(context.filesDir, PARCEL_FOLDER_NAME).also {
                if (!it.exists()) it.mkdir()
            }
        }

    private suspend fun createUniqueFile() =
        withContext(Dispatchers.IO) {
            val parcelsDir = getOrCreateParcelDir()
            // The file created isn't temporary, but it ensures a unique filename
            File.createTempFile(PARCEL_FILE_PREFIX, "", parcelsDir)
        }

    companion object {
        // Warning: changing this folder name will make users lose the paths to their parcel
        private const val PARCEL_FOLDER_NAME = "parcels"
        private const val PARCEL_FILE_PREFIX = "parcel_"
    }
}
