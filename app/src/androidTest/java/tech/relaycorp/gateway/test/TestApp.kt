package tech.relaycorp.gateway.test

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.TestAppModule

open class TestApp : App() {
    override val component: AppTestComponent = DaggerAppTestComponent.builder()
        .testAppModule(TestAppModule(this))
        .build() as AppTestComponent

    override val backgroundContext = UnconfinedTestDispatcher()

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }

    override suspend fun startPublicSyncWhenPossible() {
        // Disable automatic public sync start
    }

    override fun enqueuePublicSyncWorker() {
        // Disable public sync worker
    }
}
