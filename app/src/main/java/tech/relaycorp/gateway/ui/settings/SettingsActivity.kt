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
import kotlinx.android.synthetic.main.activity_settings.dataChart
import kotlinx.android.synthetic.main.activity_settings.dataTotal
import kotlinx.android.synthetic.main.activity_settings.learnMore
import kotlinx.android.synthetic.main.activity_settings.libraries
import kotlinx.android.synthetic.main.activity_settings.outgoingDataLayout
import kotlinx.android.synthetic.main.activity_settings.outgoingDataTitle
import kotlinx.android.synthetic.main.activity_settings.internetGateway
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
        internetGateway.setOnClickListener { openMigrateGateway() }
        learnMore.setOnClickListener { openKnowMore() }
        libraries.setOnClickListener { openLicenses() }

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
            .onEach { internetGateway.text = it }
            .launchIn(lifecycleScope)
    }

    private fun openMigrateGateway() {
        startActivityForResult(MigrateGatewayActivity.getIntent(this), REQUEST_MIGRATE_GATEWAY)
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
        private const val REQUEST_MIGRATE_GATEWAY = 101

        fun getIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
