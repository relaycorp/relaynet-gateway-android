package tech.relaycorp.gateway.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mikepenz.aboutlibraries.LibsBuilder
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_settings.dataChart
import kotlinx.android.synthetic.main.activity_settings.dataTotal
import kotlinx.android.synthetic.main.activity_settings.learnMore
import kotlinx.android.synthetic.main.activity_settings.libraries
import kotlinx.android.synthetic.main.activity_settings.outgoingDataLayout
import kotlinx.android.synthetic.main.activity_settings.outgoingDataTitle
import kotlinx.android.synthetic.main.activity_settings.publicGateway
import kotlinx.android.synthetic.main.activity_settings.publicGatewaySubmit
import kotlinx.android.synthetic.main.activity_settings.version
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.BuildConfig
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.common.format
import javax.inject.Inject

class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<SettingsViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(SettingsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_settings)
        setupNavigation()

        version.text = getString(
            R.string.settings_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString()
        )
        publicGateway.doOnTextChanged { text, _, _, _ ->
            viewModel.publicGwAddressChanged(text.toString().trim())
        }
        publicGatewaySubmit.setOnClickListener { viewModel.publicGwSubmitted() }
        learnMore.setOnClickListener { openKnowMore() }
        libraries.setOnClickListener { openLicenses() }

        viewModel
            .showOutgoingData
            .onEach {
                outgoingDataTitle.isVisible = it
                outgoingDataLayout.isVisible = it
            }
            .launchIn(lifecycleScope)

        viewModel
            .outgoingData
            .onEach {
                dataChart.progress = it.percentage
                dataTotal.text = it.total.format(this)
            }
            .launchIn(lifecycleScope)

        viewModel
            .publicGwAddress
            .onEach { publicGateway.setText(it) }
            .launchIn(lifecycleScope)

        viewModel
            .publicGwAddressEnabled
            .onEach { publicGateway.isEnabled = it }
            .launchIn(lifecycleScope)

        viewModel
            .publicGwSubmitEnabled
            .onEach { publicGatewaySubmit.isEnabled = it }
            .launchIn(lifecycleScope)

        viewModel
            .messages
            .onEach { showMessage(it) }
            .launchIn(lifecycleScope)

        viewModel
            .errors
            .onEach { showError(it) }
            .launchIn(lifecycleScope)
    }

    private fun showMessage(message: SettingsViewModel.Message) {
        messageManager.showMessage(
            when (message) {
                SettingsViewModel.Message.MigrationSuccessful ->
                    R.string.settings_pgw_migration_successful
            }
        )
    }

    private fun showError(error: SettingsViewModel.Error) {
        messageManager.showError(
            when (error) {
                SettingsViewModel.Error.MigrationFailedToResolve ->
                    R.string.settings_pgw_migration_failed_to_resolve
                SettingsViewModel.Error.MigrationFailedToRegister ->
                    R.string.settings_pgw_migration_failed_to_register
            }
        )
    }

    private fun openKnowMore() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.relaynet_website))))
    }

    private fun openLicenses() {
        LibsBuilder()
            .withActivityTitle(getString(R.string.settings_licenses))
            .withAboutIconShown(false)
            .withVersionShown(false)
            .withOwnLibsActivityClass(LicensesActivity::class.java)
            .withEdgeToEdge(true)
            .withFields(R.string::class.java.fields)
            .start(this)
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
