package tech.relaycorp.gateway.data.disk

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.relaycorp.awala.keystores.file.FileKeystoreRoot
import tech.relaycorp.gateway.App
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.nodeId
import java.io.File

class AndroidPrivateKeyStoreTest {
    private val privateKey = KeyPairSet.PRIVATE_GW.private

    @Test
    fun saveAndRetrieve() = runTest {
        val androidContext = ApplicationProvider.getApplicationContext<App>()
        val root = FileKeystoreRoot(File(androidContext.filesDir, "tmp-keystore"))
        val store = AndroidPrivateKeyStore(root, androidContext)

        store.saveIdentityKey(privateKey)
        val retrievedKey = store.retrieveIdentityKey(privateKey.nodeId)
        assertEquals(privateKey, retrievedKey)
    }

    @Test
    fun overrideKey() = runTest {
        val androidContext = ApplicationProvider.getApplicationContext<App>()
        val root = FileKeystoreRoot(File(androidContext.filesDir, "tmp-keystore"))
        val store = AndroidPrivateKeyStore(root, androidContext)

        store.saveIdentityKey(privateKey)
        store.saveIdentityKey(privateKey)

        val retrievedKey = store.retrieveIdentityKey(privateKey.nodeId)
        assertEquals(privateKey, retrievedKey)
    }
}
