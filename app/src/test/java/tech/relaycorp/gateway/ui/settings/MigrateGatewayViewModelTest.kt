package tech.relaycorp.gateway.ui.settings

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.publicsync.MigrateGateway
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals
import tech.relaycorp.gateway.ui.common.Finish
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrateGatewayViewModelTest {

    private val internetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val migrateGateway = mock<MigrateGateway>()
    private val hostnameValidator = mock<(String) -> Boolean>()
    private val viewModel = MigrateGatewayViewModel(
        internetGatewayPreferences, migrateGateway, hostnameValidator
    )

    @Before
    fun setUp() {
        runBlocking {
            whenever(internetGatewayPreferences.getAddress()).thenReturn("old.url")
            whenever(hostnameValidator(any())).thenReturn(true)
        }
    }

    @Test
    fun `when address is empty, state is Insert`() {
        viewModel.ioScope.launch {
            viewModel.addressChanged("")
            assertEquals(
                MigrateGatewayViewModel.State.Insert,
                viewModel.state.first()
            )
        }
    }

    @Test
    fun `when address is invalid, state is AddressInvalid`() {
        viewModel.ioScope.launch {
            whenever(hostnameValidator(any())).thenReturn(false)

            viewModel.addressChanged("invalid url")

            waitForAssertEquals(
                MigrateGatewayViewModel.State.Error.AddressInvalid,
                viewModel.state::first
            )
        }
    }

    @Test
    fun `when address is valid, state is AddressValid`() {
        viewModel.ioScope.launch {
            viewModel.addressChanged("new.url")
            waitForAssertEquals(
                MigrateGatewayViewModel.State.AddressValid,
                viewModel.state::first
            )
        }
    }

    @Test
    fun `when address is some as old, state is SameAddress`() {
        viewModel.ioScope.launch {
            viewModel.addressChanged("old.url")
            waitForAssertEquals(
                MigrateGatewayViewModel.State.Error.SameAddress,
                viewModel.state::first
            )
        }
    }

    @Test
    fun `when submit returns resolve error, state is FailedToResolve`() {
        viewModel.ioScope.launch {
            whenever(migrateGateway.migrate(any()))
                .thenReturn(MigrateGateway.Result.FailedToResolve)

            viewModel.addressChanged("new.url")
            viewModel.submitted()

            waitForAssertEquals(
                MigrateGatewayViewModel.State.Error.FailedToResolve,
                viewModel.state::first
            )
        }
    }

    @Test
    fun `when submit is successful, finishes successfully`() {
        viewModel.ioScope.launch {
            whenever(migrateGateway.migrate(any())).thenReturn(MigrateGateway.Result.Successful)

            val finishes = mutableListOf<Finish>()
            viewModel.finishSuccessfully.onEach { finishes.add(it) }.collect()

            viewModel.addressChanged("new.url")
            viewModel.submitted()

            assertTrue(finishes.any())
        }
    }
}
