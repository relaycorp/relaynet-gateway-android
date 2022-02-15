package tech.relaycorp.gateway.data.disk

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.relaycorp.awala.keystores.file.FileKeystoreRoot
import tech.relaycorp.gateway.App
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.privateAddress
import java.io.File

class AndroidPrivateKeyStoreTest {
    private val privateKey = KeyPairSet.PRIVATE_GW.private

    @Test
    fun saveAndRetrieve() = runBlockingTest {
        val androidContext = ApplicationProvider.getApplicationContext<App>()
        val root = FileKeystoreRoot(File(androidContext.filesDir, "tmp-keystore"))
        val store = AndroidPrivateKeyStore(root, androidContext)

        store.saveIdentityKey(privateKey)
        val retrievedKey = store.retrieveIdentityKey(privateKey.privateAddress)
        assertEquals(privateKey, retrievedKey)
    }

    @Test
    fun overrideKey() = runBlockingTest {
        val androidContext = ApplicationProvider.getApplicationContext<App>()
        val root = FileKeystoreRoot(File(androidContext.filesDir, "tmp-keystore"))
        val store = AndroidPrivateKeyStore(root, androidContext)

        store.saveIdentityKey(privateKey)
        store.saveIdentityKey(privateKey)

        val retrievedKey = store.retrieveIdentityKey(privateKey.privateAddress)
        assertEquals(privateKey, retrievedKey)
    }
}
