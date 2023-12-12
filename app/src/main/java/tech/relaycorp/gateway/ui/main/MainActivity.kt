package tech.relaycorp.gateway.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.databinding.ActivityMainBinding
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
        ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setTitle(R.string.app_name)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settings.setOnClickListener {
            startActivity(SettingsActivity.getIntent(this))
        }
        binding.courierConnection.setOnClickListener {
            startActivity(CourierConnectionActivity.getIntent(this))
        }
        binding.courierSync.setOnClickListener {
            startActivity(CourierSyncActivity.getIntent(this))
        }
        binding.vpn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.vpn_app))))
        }
        binding.getHelp.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.gateway_help))))
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
                binding.image.setImageResource(state.toImageRes())
                binding.titleText.setText(state.toTitleRes())
                binding.messageText.setText(state.toTextRes())
                binding.messageText.gravity = state.toTextGravity()
                binding.internetWithoutGatewayButtonsLayout.isVisible =
                    state is ConnectionState.InternetWithoutGateway
                binding.courierConnection.isVisible =
                    state !is ConnectionState.InternetWithGateway &&
                    state !is ConnectionState.WiFiWithCourier &&
                    state !is ConnectionState.InternetWithoutGateway
                binding.courierSync.isVisible = state is ConnectionState.WiFiWithCourier
            }
            .launchIn(lifecycleScope)
    }

    private fun ConnectionState.toImageRes() = when (this) {
        ConnectionState.InternetWithGateway -> R.drawable.main_connected_image
        is ConnectionState.WiFiWithCourier -> R.drawable.main_courier_image
        else -> R.drawable.main_disconnected_image
    }

    private fun ConnectionState.toTitleRes() = when (this) {
        is ConnectionState.InternetWithoutGateway ->
            R.string.main_status_internet_no_gateway
        ConnectionState.InternetWithGateway ->
            R.string.main_status_internet
        is ConnectionState.WiFiWithCourier ->
            R.string.main_status_courier
        else ->
            R.string.main_status_disconnected
    }

    private fun ConnectionState.toTextRes() = when (this) {
        is ConnectionState.InternetWithoutGateway ->
            R.string.main_status_internet_no_gateway_text
        ConnectionState.InternetWithGateway ->
            R.string.main_status_internet_text
        is ConnectionState.WiFiWithCourier ->
            R.string.main_status_courier_text
        else ->
            R.string.main_status_disconnected_text
    }

    private fun ConnectionState.toTextGravity() = when (this) {
        ConnectionState.InternetWithGateway,
        is ConnectionState.WiFiWithCourier,
        -> Gravity.CENTER_HORIZONTAL
        else -> Gravity.START
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
