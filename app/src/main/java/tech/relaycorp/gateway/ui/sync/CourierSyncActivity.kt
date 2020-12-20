package tech.relaycorp.gateway.ui.sync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_courier_sync.animation
import kotlinx.android.synthetic.main.activity_courier_sync.close
import kotlinx.android.synthetic.main.activity_courier_sync.image
import kotlinx.android.synthetic.main.activity_courier_sync.stateMessage
import kotlinx.android.synthetic.main.activity_courier_sync.stop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.domain.courier.CourierSync
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.common.startLoopingAvd
import tech.relaycorp.gateway.ui.common.stopLoopingAvd
import javax.inject.Inject

class CourierSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<CourierSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(CourierSyncViewModel::class.java)
    }

    private var stopConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_courier_sync)

        stop.setOnClickListener { showStopConfirmDialog() }
        close.setOnClickListener { finish() }

        viewModel
            .state
            .onEach { stateMessage.setText(it.toStringRes()) }
            .map { it != CourierSync.State.Finished && it != CourierSync.State.Error }
            .distinctUntilChanged()
            .onEach { isSyncing ->
                stop.isVisible = isSyncing
                close.isVisible = !isSyncing

                image.setImageResource(
                    if (isSyncing) {
                        R.drawable.sync_image
                    } else {
                        R.drawable.sync_done_image
                    }
                )
                if (isSyncing) {
                    animation.startLoopingAvd(R.drawable.sync_animation)
                } else {
                    animation.stopLoopingAvd()
                }
            }
            .onCompletion { animation.stopLoopingAvd() }
            .launchIn(lifecycleScope)

        viewModel
            .finish
            .onEach { finish() }
            .launchIn(lifecycleScope)
    }

    private fun showStopConfirmDialog() {
        stopConfirmDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sync_stop_confirm_title)
            .setMessage(R.string.sync_internet_stop_confirm_message)
            .setPositiveButton(R.string.stop) { _, _ -> viewModel.stopClicked() }
            .setNegativeButton(R.string.continue_, null)
            .show()
    }

    private fun CourierSync.State.toStringRes() =
        when (this) {
            CourierSync.State.DeliveringCargo -> R.string.sync_delivering_cargo
            CourierSync.State.Waiting -> R.string.sync_waiting
            CourierSync.State.CollectingCargo -> R.string.sync_collecting_cargo
            CourierSync.State.Finished -> R.string.sync_finished
            CourierSync.State.Error -> R.string.sync_error
        }

    companion object {
        fun getIntent(context: Context) = Intent(context, CourierSyncActivity::class.java)
    }
}
