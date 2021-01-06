package tech.relaycorp.gateway.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_migrate_gateway.address
import kotlinx.android.synthetic.main.activity_migrate_gateway.addressLayout
import kotlinx.android.synthetic.main.activity_migrate_gateway.info
import kotlinx.android.synthetic.main.activity_migrate_gateway.submit
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.common.getColorFromAttr
import javax.inject.Inject

class MigrateGatewayActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<MigrateGatewayViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MigrateGatewayViewModel::class.java)
    }

    private var confirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setTitle(R.string.settings)
        setContentView(R.layout.activity_migrate_gateway)
        setupNavigation(R.drawable.ic_back)

        address.addTextChangedListener(
            onTextChanged = { text: CharSequence?, _, _, _ ->
                viewModel.addressChanged(text?.toString()?.trim() ?: "")
            }
        )
        submit.setOnClickListener { showSubmitConfirm() }

        viewModel
            .state
            .onEach { onStateChange(it) }
            .launchIn(lifecycleScope)

        viewModel
            .finishSuccessfully
            .onEach {
                setResult(RESULT_MIGRATION_SUCCESSFUL)
                finish()
            }
            .launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (confirmDialog?.isShowing == true) {
            confirmDialog?.dismiss()
        }
        confirmDialog = null
    }

    private fun onStateChange(state: MigrateGatewayViewModel.State) {
        address.isEnabled = state !is MigrateGatewayViewModel.State.Submitting

        addressLayout.endIconDrawable = when (state) {
            MigrateGatewayViewModel.State.AddressValid ->
                ContextCompat.getDrawable(this, R.drawable.ic_check)
            MigrateGatewayViewModel.State.Error.SameAddress,
            MigrateGatewayViewModel.State.Error.FailedToResolve,
            MigrateGatewayViewModel.State.Error.FailedToRegister ->
                ContextCompat.getDrawable(this, R.drawable.ic_close)
            else -> null
        }
        val infoColor = getColorFromAttr(
            when (state) {
                MigrateGatewayViewModel.State.AddressValid -> R.attr.colorSecondary
                is MigrateGatewayViewModel.State.Error -> R.attr.colorError
                else -> R.attr.colorOnBackground
            }
        )
        addressLayout.setEndIconTintList(ColorStateList.valueOf(infoColor))

        info.text = when (state) {
            MigrateGatewayViewModel.State.Insert ->
                R.string.settings_pgw_insert
            MigrateGatewayViewModel.State.AddressValid ->
                R.string.settings_pgw_valid
            MigrateGatewayViewModel.State.Submitting ->
                R.string.settings_pgw_migrating
            MigrateGatewayViewModel.State.Error.SameAddress ->
                R.string.settings_pgw_migration_same_address
            MigrateGatewayViewModel.State.Error.FailedToResolve ->
                R.string.settings_pgw_migration_failed_to_resolve
            MigrateGatewayViewModel.State.Error.FailedToRegister ->
                R.string.settings_pgw_migration_failed_to_register
            else -> null
        }?.let(this::getString).orEmpty()
        info.setTextColor(infoColor)

        submit.isVisible = when (state) {
            MigrateGatewayViewModel.State.Insert,
            MigrateGatewayViewModel.State.Error.SameAddress,
            MigrateGatewayViewModel.State.Error.AddressInvalid ->
                false
            MigrateGatewayViewModel.State.AddressValid,
            MigrateGatewayViewModel.State.Submitting,
            MigrateGatewayViewModel.State.Error.FailedToResolve,
            MigrateGatewayViewModel.State.Error.FailedToRegister ->
                true
        }

        submit.isEnabled = state !is MigrateGatewayViewModel.State.Submitting
    }

    private fun showSubmitConfirm() {
        confirmDialog = AlertDialog.Builder(this)
            .setMessage(R.string.settings_pgw_migrate_confirm)
            .setNeutralButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_) { _, _ -> viewModel.submitted() }
            .show()
    }

    companion object {
        const val RESULT_MIGRATION_SUCCESSFUL = 11

        fun getIntent(context: Context) = Intent(context, MigrateGatewayActivity::class.java)
    }
}
