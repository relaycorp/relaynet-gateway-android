package tech.relaycorp.gateway.common.di

import dagger.Subcomponent
import tech.relaycorp.gateway.ui.main.MainActivity
import tech.relaycorp.gateway.ui.onboarding.OnboardingActivity
import tech.relaycorp.gateway.ui.settings.MigrateGatewayActivity
import tech.relaycorp.gateway.ui.settings.SettingsActivity
import tech.relaycorp.gateway.ui.sync.CourierConnectionActivity
import tech.relaycorp.gateway.ui.sync.CourierSyncActivity

@PerActivity
@Subcomponent
interface ActivityComponent {

    // Activities

    fun inject(activity: CourierConnectionActivity)
    fun inject(activity: CourierSyncActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: MigrateGatewayActivity)
    fun inject(activity: OnboardingActivity)
    fun inject(activity: SettingsActivity)
}
