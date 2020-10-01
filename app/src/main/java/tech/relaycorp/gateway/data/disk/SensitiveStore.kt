package tech.relaycorp.gateway.data.disk

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.common.Logging.logger
import java.io.File
import java.io.IOException
import java.util.logging.Level
import javax.inject.Inject

class SensitiveStore
@Inject constructor(
    private val context: Context
) {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    suspend fun store(location: String, data: ByteArray) {
        withContext(Dispatchers.IO) {
            buildFile(location).delete()
            buildEncryptedFile(location)
                .openFileOutput()
                .use { it.write(data) }
        }
    }

    suspend fun read(location: String) = withContext(Dispatchers.IO) {
        try {
            buildEncryptedFile(location)
                .openFileInput()
                .use { it.readBytes() }
        } catch (exception: IOException) {
            logger.log(Level.INFO, "SensitiveStore read", exception)
            null
        }
    }

    private fun buildFile(location: String) = File(context.filesDir, location)

    private fun buildEncryptedFile(location: String) =
        EncryptedFile.Builder(
            context,
            buildFile(location),
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
}
