package tech.relaycorp.gateway.data.disk

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.relaycorp.gateway.App
import java.io.File
import java.nio.charset.Charset

class SensitiveStoreTest {

    private lateinit var sensitiveStore: SensitiveStore

    private val folder by lazy {
        File(ApplicationProvider.getApplicationContext<App>().filesDir, "test").also { it.mkdirs() }
    }

    @Before
    fun setUp() {
        sensitiveStore = SensitiveStore(ApplicationProvider.getApplicationContext<App>())
    }

    @After
    fun tearDown() {
        folder.deleteRecursively()
    }

    @Test
    fun storeAndRead() {
        runBlocking {
            val store = SensitiveStore(InstrumentationRegistry.getInstrumentation().targetContext)
            val message = "ABC123"
            val fileName = "${folder.name}/file"
            store.store(fileName, message.toByteArray(Charset.defaultCharset()))
            val readData = store.read(fileName)
            assertEquals(
                message,
                readData!!.toString(Charset.defaultCharset())
            )
        }
    }
}
