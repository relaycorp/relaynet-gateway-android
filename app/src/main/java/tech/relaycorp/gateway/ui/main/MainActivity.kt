package tech.relaycorp.gateway.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_main.courierConnection
import kotlinx.android.synthetic.main.activity_main.courierSync
import kotlinx.android.synthetic.main.activity_main.image
import kotlinx.android.synthetic.main.activity_main.messageText
import kotlinx.android.synthetic.main.activity_main.settings
import kotlinx.android.synthetic.main.activity_main.titleText
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.onboarding.OnboardingActivity
import tech.relaycorp.gateway.ui.settings.SettingsActivity
import tech.relaycorp.gateway.ui.sync.CourierConnectionActivity
import tech.relaycorp.gateway.ui.sync.CourierSyncActivity
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
        setTitle(R.string.app_name)
        setContentView(R.layout.activity_main)

        settings.setOnClickListener {
            startActivity(SettingsActivity.getIntent(this))
        }
        courierConnection.setOnClickListener {
            startActivity(CourierConnectionActivity.getIntent(this))
        }
        courierSync.setOnClickListener {
            startActivity(CourierSyncActivity.getIntent(this))
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
                image.setImageResource(state.toImageRes())
                titleText.setText(state.toTitleRes())
                messageText.setText(state.toTextRes())
                messageText.gravity = state.toTextGravity()
                courierConnection.isVisible =
                    state !is ConnectionState.InternetAndPublicGateway &&
                    state !is ConnectionState.WiFiWithCourier
                courierSync.isVisible = state is ConnectionState.WiFiWithCourier
            }
            .launchIn(lifecycleScope)
    }

    private fun ConnectionState.toImageRes() =
        when (this) {
            ConnectionState.InternetAndPublicGateway -> R.drawable.main_connected_image
            is ConnectionState.WiFiWithCourier -> R.drawable.main_courier_image
            else -> R.drawable.main_disconnected_image
        }

    private fun ConnectionState.toTitleRes() =
        when (this) {
            ConnectionState.InternetAndPublicGateway -> R.string.main_status_internet
            is ConnectionState.WiFiWithCourier -> R.string.main_status_courier
            else -> R.string.main_status_disconnected
        }

    private fun ConnectionState.toTextRes() =
        when (this) {
            ConnectionState.InternetAndPublicGateway -> R.string.main_status_internet_text
            is ConnectionState.WiFiWithCourier -> R.string.main_status_courier_text
            else -> R.string.main_status_disconnected_text
        }

    private fun ConnectionState.toTextGravity() =
        when (this) {
            ConnectionState.InternetAndPublicGateway,
            is ConnectionState.WiFiWithCourier -> Gravity.CENTER_HORIZONTAL
            else -> Gravity.START
        }

    companion object {
        fun getIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
