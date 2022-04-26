package tech.relaycorp.gateway.test

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.relaynet.keystores.CertificateStore
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.pki.CertificationPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.io.File
import javax.inject.Inject

class KeystoreResetTestRule : TestRule {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var privateKeyStore: PrivateKeyStore

    @Inject
    lateinit var publicGatewayPreferences: PublicGatewayPreferences

    @Inject
    lateinit var certificateStore: CertificateStore

    override fun apply(base: Statement, description: Description?) =
        object : Statement() {
            override fun evaluate() {
                AppTestProvider.component.inject(this@KeystoreResetTestRule)

                val keystoresFile = File("${context.filesDir}/keystores")
                keystoresFile.deleteRecursively()
                runBlocking {
                    privateKeyStore.saveIdentityKey(KeyPairSet.PRIVATE_GW.private)
                    certificateStore.save(
                        CertificationPath(
                            PDACertPath.PRIVATE_GW,
                            emptyList()
                        ),
                        publicGatewayPreferences.getPrivateAddress()
                    )
                }

                base.evaluate()

                keystoresFile.deleteRecursively()
            }
        }
}
