package tech.relaycorp.gateway.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mikepenz.aboutlibraries.LibsBuilder
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.BuildConfig
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.databinding.ActivitySettingsBinding
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.common.format
import javax.inject.Inject

class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<SettingsViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(SettingsViewModel::class.java)
    }

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()

        binding.version.text = getString(
            R.string.settings_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString(),
        )
        binding.internetGateway.setOnClickListener { openMigrateGateway() }
        binding.learnMore.setOnClickListener { openKnowMore() }
        binding.libraries.setOnClickListener { openLicenses() }

        results
            .onEach {
                if (it.requestCode == REQUEST_MIGRATE_GATEWAY &&
                    it.resultCode == MigrateGatewayActivity.RESULT_MIGRATION_SUCCESSFUL
                ) {
                    messageManager.showMessage(R.string.settings_pgw_migration_successful)
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .showOutgoingData
            .onEach {
                binding.outgoingDataTitle.isVisible = it
                binding.outgoingDataLayout.isVisible = it
            }
            .launchIn(lifecycleScope)

        viewModel
            .outgoingData
            .onEach {
                binding.dataChart.progress = it.percentage
                binding.dataTotal.text = it.total.format(this)
            }
            .launchIn(lifecycleScope)

        viewModel
            .publicGwAddress
            .onEach { binding.internetGateway.text = it }
            .launchIn(lifecycleScope)
    }

    private fun openMigrateGateway() {
        startActivityForResult(MigrateGatewayActivity.getIntent(this), REQUEST_MIGRATE_GATEWAY)
    }

    private fun openKnowMore() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.awala_website))))
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
        private const val REQUEST_MIGRATE_GATEWAY = 101

        fun getIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
