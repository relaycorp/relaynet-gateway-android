package tech.relaycorp.gateway.ui.sync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.databinding.ActivityCourierConnectionBinding
import tech.relaycorp.gateway.ui.BaseActivity
import javax.inject.Inject

class CourierConnectionActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<CourierConnectionViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(CourierConnectionViewModel::class.java)
    }

    private lateinit var binding: ActivityCourierConnectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        binding = ActivityCourierConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()

        binding.startSync.setOnClickListener { openCourierSync() }
        binding.wifiSettings.setOnClickListener { openWifiSettings() }

        viewModel
            .state
            .onEach {
                binding.disconnectedLayout.isVisible = it !is ConnectionState.WiFiWithCourier
                binding.connectedLayout.isVisible = it is ConnectionState.WiFiWithCourier
            }
            .launchIn(lifecycleScope)
    }

    private fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun openCourierSync() {
        startActivity(CourierSyncActivity.getIntent(this))
        finish()
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, CourierConnectionActivity::class.java)
    }
}
