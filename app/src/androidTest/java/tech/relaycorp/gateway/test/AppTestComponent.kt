package tech.relaycorp.gateway.test

import dagger.Component
import tech.relaycorp.gateway.AppModule
import tech.relaycorp.gateway.background.endpoint.EndpointPreRegistrationServiceTest
import tech.relaycorp.gateway.background.endpoint.GatewaySyncServiceParcelDeliveryTest
import tech.relaycorp.gateway.common.di.AppComponent
import tech.relaycorp.gateway.data.DataModule
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, DataModule::class])
interface AppTestComponent : AppComponent {
    fun inject(app: TestApp)

    // Tests

    fun inject(test: EndpointPreRegistrationServiceTest)
    fun inject(test: GatewaySyncServiceParcelDeliveryTest)
}
