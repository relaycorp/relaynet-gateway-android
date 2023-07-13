package tech.relaycorp.gateway.test

import dagger.Component
import tech.relaycorp.gateway.TestAppModule
import tech.relaycorp.gateway.background.endpoint.EndpointPreRegistrationServiceTest
import tech.relaycorp.gateway.background.endpoint.GatewaySyncServiceParcelCollectionTest
import tech.relaycorp.gateway.background.endpoint.GatewaySyncServiceParcelDeliveryTest
import tech.relaycorp.gateway.common.di.AppComponent
import tech.relaycorp.gateway.data.DataModule
import tech.relaycorp.gateway.ui.main.MainActivityTest
import tech.relaycorp.gateway.ui.settings.SettingsActivityTest
import javax.inject.Singleton

@Singleton
@Component(modules = [TestAppModule::class, DataModule::class])
interface AppTestComponent : AppComponent {
    fun inject(app: TestApp)

    // Tests

    fun inject(test: ClearTestDatabaseRule)
    fun inject(test: KeystoreResetTestRule)
    fun inject(test: EndpointPreRegistrationServiceTest)
    fun inject(test: GatewaySyncServiceParcelDeliveryTest)
    fun inject(test: GatewaySyncServiceParcelCollectionTest)
    fun inject(test: MainActivityTest)
    fun inject(test: SettingsActivityTest)
}
