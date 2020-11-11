package tech.relaycorp.gateway.background

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundAppMonitor
@Inject constructor() : Application.ActivityLifecycleCallbacks {

    private val activityCountFlow = MutableStateFlow(0)

    fun observe() = activityCountFlow.map { if (it == 0) State.Background else State.Foreground }

    override fun onActivityStarted(activity: Activity) {
        activityCountFlow.value++
    }

    override fun onActivityStopped(activity: Activity) {
        activityCountFlow.value--
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit

    enum class State {
        Foreground, Background
    }
}
