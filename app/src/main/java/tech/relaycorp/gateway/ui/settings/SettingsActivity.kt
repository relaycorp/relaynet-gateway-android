package tech.relaycorp.gateway.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.mikepenz.aboutlibraries.LibsBuilder
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_settings.learnMore
import kotlinx.android.synthetic.main.activity_settings.libraries
import kotlinx.android.synthetic.main.activity_settings.version
import tech.relaycorp.gateway.BuildConfig
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.BaseActivity
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
        learnMore.setOnClickListener { openKnowMore() }
        libraries.setOnClickListener { openLicenses() }
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
