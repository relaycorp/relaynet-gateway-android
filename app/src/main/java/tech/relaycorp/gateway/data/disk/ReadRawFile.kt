package tech.relaycorp.gateway.data.disk

import android.content.res.Resources
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import javax.inject.Inject

class ReadRawFile
@Inject constructor(
    private val resources: Resources,
) {
    suspend fun read(@RawRes fileRes: Int) = withContext(Dispatchers.IO) {
        resources.openRawResource(fileRes).readBytesAndClose()
    }
}
