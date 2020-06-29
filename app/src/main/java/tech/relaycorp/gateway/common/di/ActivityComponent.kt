package tech.relaycorp.gateway.common.di

import dagger.Subcomponent
import tech.relaycorp.gateway.ui.main.MainActivity

@PerActivity
@Subcomponent
interface ActivityComponent {

    // Activities

    fun inject(activity: MainActivity)
}
