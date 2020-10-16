package tech.relaycorp.gateway.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_main.appsState
import kotlinx.android.synthetic.main.activity_main.dataLayout
import kotlinx.android.synthetic.main.activity_main.dataState
import kotlinx.android.synthetic.main.activity_main.networkState
import kotlinx.android.synthetic.main.activity_main.syncCourier
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.common.format
import tech.relaycorp.gateway.ui.onboarding.OnboardingActivity
import tech.relaycorp.gateway.ui.sync.CourierConnectionActivity
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<MainViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setTitle(R.string.main_title)
        setContentView(R.layout.activity_main)

        syncCourier.setOnClickListener {
            startActivity(CourierConnectionActivity.getIntent(this))
        }

        viewModel
            .openOnboarding
            .onEach {
                startActivity(OnboardingActivity.getIntent(this))
                finish()
            }
            .launchIn(lifecycleScope)

        viewModel
            .connectionState
            .onEach { state ->
                networkState.setText(state.toTitleRes())
            }
            .launchIn(lifecycleScope)

        viewModel
            .dataState
            .onEach { state ->
                dataLayout.isVisible = state is MainViewModel.DataState.Visible
                if (state is MainViewModel.DataState.Visible) {
                    dataState.text = state.toText()
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .appsState
            .onEach { state ->
                appsState.setText(
                    when (state) {
                        MainViewModel.AppsState.None -> R.string.main_apps_none
                        MainViewModel.AppsState.Some -> R.string.main_apps_some
                    }
                )
            }
            .launchIn(lifecycleScope)

        viewModel
            .isCourierSyncVisible
            .onEach {
                syncCourier.isVisible = it
            }
            .launchIn(lifecycleScope)
    }

    private fun ConnectionState.toTitleRes() =
        when (this) {
            ConnectionState.InternetAndPublicGateway -> R.string.main_status_internet
            is ConnectionState.WiFiWithCourier -> R.string.main_status_courier
            else -> R.string.main_status_disconnected
        }

    private fun MainViewModel.DataState.Visible.toText() =
        when (this) {
            MainViewModel.DataState.Visible.WithoutOutgoingData ->
                getString(R.string.main_data_to_sync_none)
            is MainViewModel.DataState.Visible.WithOutgoingData ->
                getString(
                    R.string.main_data_to_sync_some,
                    dataWaitingToSync.format(this@MainActivity)
                )
        }

    companion object {
        fun getIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
