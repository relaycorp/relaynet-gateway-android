package tech.relaycorp.gateway.data.disk

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.CargoDeliveryCertPath
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.gateway.test.factory.CargoFactory
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.ZonedDateTime
import kotlin.test.assertEquals

internal class CargoStorageTest {

    private val diskOperations = mock<DiskMessageOperations>()
    private val mockLocalConfig = mock<LocalConfig>()
    private val cargoStorage = CargoStorage(diskOperations, mockLocalConfig)

    @Test
    internal fun `Malformed cargo should be refused`() = runBlockingTest {
        assertThrows<CargoStorage.Exception.MalformedCargo> {
            runBlocking {
                cargoStorage.store(ByteArray(0).inputStream())
            }
        }

        verify(diskOperations, never()).writeMessage(anyString(), anyString(), any())
    }

    @Test
    fun `Valid cargo bound for a public gateway should be refused`() = runBlockingTest {
        whenever(mockLocalConfig.getCertificate()).thenReturn(CargoDeliveryCertPath.PRIVATE_GW)

        val cargo = Cargo(
            "https://foo.relaycorp.tech",
            "".toByteArray(),
            CargoDeliveryCertPath.PUBLIC_GW
        )

        assertThrows<CargoStorage.Exception.InvalidCargo> {
            runBlocking {
                cargoStorage
                    .store(cargo.serialize(KeyPairSet.PUBLIC_GW.private).inputStream())
            }
        }

        verify(diskOperations, never()).writeMessage(anyString(), anyString(), any())
    }

    @Test
    fun `Well-formed but unauthorized cargo should be refused`() = runBlockingTest {
        whenever(mockLocalConfig.getCertificate()).thenReturn(CargoDeliveryCertPath.PRIVATE_GW)

        val unauthorizedSenderKeyPair = generateRSAKeyPair()
        val unauthorizedSenderCert = issueGatewayCertificate(
            unauthorizedSenderKeyPair.public,
            unauthorizedSenderKeyPair.private,
            ZonedDateTime.now().plusMinutes(1)
        )
        val cargo = Cargo(
            CargoDeliveryCertPath.PRIVATE_GW.subjectPrivateAddress,
            "".toByteArray(),
            unauthorizedSenderCert
        )

        assertThrows<CargoStorage.Exception.InvalidCargo> {
            runBlocking {
                cargoStorage.store(cargo.serialize(unauthorizedSenderKeyPair.private).inputStream())
            }
        }

        verify(diskOperations, never()).writeMessage(anyString(), anyString(), any())
    }

    @Test
    fun `Authorized cargo should be accepted`() = runBlockingTest {
        whenever(mockLocalConfig.getCertificate()).thenReturn(CargoDeliveryCertPath.PRIVATE_GW)
        val cargoSerialized = CargoFactory.buildSerialized()

        cargoStorage.store(cargoSerialized.inputStream())

        argumentCaptor<ByteArray>().apply {
            verify(diskOperations).writeMessage(
                eq(CargoStorage.FOLDER),
                eq(CargoStorage.PREFIX),
                capture()
            )

            assertEquals(cargoSerialized.asList(), firstValue.asList())
        }
    }
}
