package tech.relaycorp.gateway.data.disk

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.relaycorp.gateway.App
import java.io.File
import java.nio.charset.Charset

class FileStoreTest {

    private lateinit var store: FileStore

    private val folder by lazy {
        File(ApplicationProvider.getApplicationContext<App>().filesDir, "test").also { it.mkdirs() }
    }

    @Before
    fun setUp() {
        store = FileStore(ApplicationProvider.getApplicationContext<App>())
    }

    @After
    fun tearDown() {
        folder.deleteRecursively()
    }

    @Test
    fun storeAndRead() {
        runBlocking {
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

    @Test
    fun storeAndUpdate() {
        runBlocking {
            val message1 = "1"
            val message2 = "2"
            val fileName = "${folder.name}/file"
            store.store(fileName, message1.toByteArray(Charset.defaultCharset()))
            store.store(fileName, message2.toByteArray(Charset.defaultCharset()))
            val readData = store.read(fileName)
            assertEquals(
                message2,
                readData!!.toString(Charset.defaultCharset())
            )
        }
    }

    @Test
    fun delete() {
        runBlocking {
            val fileName = "${folder.name}/file"
            assertNull(store.read(fileName))
            store.store(fileName, "1234".toByteArray())
            assertNotNull(store.read(fileName))
            store.delete(fileName)
            assertNull(store.read(fileName))
        }
    }
}
