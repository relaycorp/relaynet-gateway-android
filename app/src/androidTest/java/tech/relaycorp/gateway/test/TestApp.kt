package tech.relaycorp.gateway.test

import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.AppModule
import tech.relaycorp.gateway.TestAppModule

class TestApp : App() {
    override val component: AppTestComponent = DaggerAppTestComponent.builder()
        .testAppModule(TestAppModule(this))
        .build() as AppTestComponent

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }

    override fun startPublicSyncWhenPossible() {
        // Disable automatic public sync start
    }

    override fun enqueuePublicSyncWorker() {
        // Disable public sync worker
    }
}
