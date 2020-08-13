package tech.relaycorp.gateway.ui.main

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_main.dataState
import kotlinx.android.synthetic.main.activity_main.networkState
import kotlinx.android.synthetic.main.activity_main.syncCourier
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.common.format
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
            .connectionState
            .onEach { state ->
                networkState.text = state.javaClass.simpleName
            }
            .launchIn(lifecycleScope)

        viewModel
            .dataToSyncState
            .onEach { state ->
                dataState.isVisible = state is MainViewModel.DataToSyncState.Visible
                if (state is MainViewModel.DataToSyncState.Visible) {
                    dataState.text = state.toText()
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .isCourierSyncVisible
            .onEach {
                syncCourier.isVisible = it
            }
            .launchIn(lifecycleScope)
    }

    private fun MainViewModel.DataToSyncState.Visible.toText() =
        when (this) {
            MainViewModel.DataToSyncState.Visible.WithoutApplications ->
                getString(R.string.main_no_apps)
            is MainViewModel.DataToSyncState.Visible.WithApplications ->
                if (dataWaitingToSync.isZero) {
                    getString(R.string.main_data_to_sync_none)
                } else {
                    getString(
                        R.string.main_data_to_sync_some,
                        dataWaitingToSync.format(this@MainActivity)
                    )
                }
        }
}
