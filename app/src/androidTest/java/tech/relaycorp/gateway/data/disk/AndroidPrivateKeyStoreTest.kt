package tech.relaycorp.gateway.data.disk

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.relaycorp.awala.keystores.file.FileKeystoreRoot
import tech.relaycorp.gateway.App
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.io.File

class AndroidPrivateKeyStoreTest {
    @Test
    fun saveAndRetrieve() = runBlockingTest {
        val androidContext = ApplicationProvider.getApplicationContext<App>()
        val root = FileKeystoreRoot(File(androidContext.filesDir, "tmp-keystore"))
        val store = AndroidPrivateKeyStore(root, androidContext)
        val privateKey = KeyPairSet.PRIVATE_ENDPOINT.private
        val certificate = PDACertPath.PRIVATE_ENDPOINT

        store.saveIdentityKey(privateKey, certificate)
        val retrievedKeypair = store.retrieveIdentityKey(certificate.subjectPrivateAddress)
        assertEquals(privateKey, retrievedKeypair.privateKey)
        assertEquals(certificate, retrievedKeypair.certificate)
    }
}
