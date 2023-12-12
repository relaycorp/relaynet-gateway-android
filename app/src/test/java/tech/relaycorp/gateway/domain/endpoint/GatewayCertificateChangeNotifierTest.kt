package tech.relaycorp.gateway.domain.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.test.factory.LocalEndpointFactory

class GatewayCertificateChangeNotifierTest {

    private val notifyEndpoints = mock<NotifyEndpoints>()
    private val endpointDao = mock<LocalEndpointDao>()

    @Test
    fun `notify all local endpoints are registered`() = runBlockingTest {
        // Arrange
        val useCase = build()

        val listOfEndpoints = listOf(
            LocalEndpointFactory.build(),
            LocalEndpointFactory.build(),
            LocalEndpointFactory.build(),
        )

        whenever(endpointDao.list()).thenReturn(listOfEndpoints)

        // Act
        useCase.notifyAll()

        // Assert
        verify(notifyEndpoints, times(1)).notify(
            listOfEndpoints,
            NotificationType.GatewayCertificateChange,
        )
    }

    @Test
    fun `don't Notify if no endpoints are registered`() = runBlockingTest {
        // Arrange
        val useCase = build()

        whenever(endpointDao.list()).thenReturn(emptyList())

        // Act
        useCase.notifyAll()

        // Assert
        verify(notifyEndpoints, never()).notify(
            any<List<LocalEndpoint>>(),
            eq(NotificationType.GatewayCertificateChange),
        )
    }

    fun build() = GatewayCertificateChangeNotifier(
        notifyEndpoints,
        endpointDao,
    )
}
