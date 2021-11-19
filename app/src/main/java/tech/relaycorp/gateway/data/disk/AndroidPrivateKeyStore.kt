package tech.relaycorp.gateway.data.disk

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import tech.relaycorp.awala.keystores.file.FileKeystoreRoot
import tech.relaycorp.awala.keystores.file.FilePrivateKeyStore
import java.io.File

internal class AndroidPrivateKeyStore(
    root: FileKeystoreRoot,
    private val context: Context
) : FilePrivateKeyStore(root) {
    override fun makeEncryptedInputStream(file: File) = buildEncryptedFile(file).openFileInput()

    override fun makeEncryptedOutputStream(file: File) = buildEncryptedFile(file).openFileOutput()

    private fun buildEncryptedFile(file: File) =
        EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

    private val masterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    companion object {
        private const val MASTER_KEY_ALIAS = "_gateway_master_key_"
    }
}
