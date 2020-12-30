package tech.relaycorp.gateway.ui.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    appPreferences: AppPreferences,
    private val connectionStateObserver: ConnectionStateObserver
) : BaseViewModel() {

    // Outputs

    val openOnboarding: Flow<Unit> get() = _openOnboarding.filterNotNull()
    private val _openOnboarding = MutableStateFlow<Unit?>(null)

    val connectionState get() = connectionStateObserver.observe()

    init {
        appPreferences
            .isOnboardingDone()
            .take(1)
            .onEach { isDone -> if (!isDone) _openOnboarding.value = Unit }
            .launchIn(ioScope)
    }
}
