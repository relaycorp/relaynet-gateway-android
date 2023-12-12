package tech.relaycorp.gateway.data.preference

import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import javax.inject.Inject
import javax.inject.Provider

class AppPreferences
@Inject constructor(
    private val preferences: Provider<FlowSharedPreferences>,
) {

    // Address

    private val onboardingDone by lazy {
        preferences.get().getBoolean("onboarding_done", false)
    }

    fun isOnboardingDone() = { onboardingDone }.toFlow()
    suspend fun setOnboardingDone(value: Boolean) = onboardingDone.setAndCommit(value)
}
