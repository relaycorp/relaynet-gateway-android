package tech.relaycorp.gateway

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.MutableStateFlow
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.test.TestApp
import javax.inject.Singleton

@Module
class TestAppModule(
    app: TestApp
) : AppModule(app) {

    @Provides
    @Singleton
    fun connectionState(): MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.InternetAndPublicGateway)

    @Provides
    fun mockedConnectStateObserver(
        connectionStateFlow: MutableStateFlow<ConnectionState>
    ): ConnectionStateObserver {
        val mock = mock<ConnectionStateObserver>()
        whenever(mock.observe()).thenReturn(connectionStateFlow)
        return mock
    }
}
