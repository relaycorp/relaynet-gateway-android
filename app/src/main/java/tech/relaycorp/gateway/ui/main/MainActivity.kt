package tech.relaycorp.gateway.ui.main

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.networkState
import kotlinx.android.synthetic.main.activity_main.syncCourier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.sync.CourierConnectionActivity
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject
    lateinit var connectionStateObserver: ConnectionStateObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setTitle(R.string.main_title)
        setContentView(R.layout.activity_main)

        syncCourier.setOnClickListener {
            startActivity(CourierConnectionActivity.getIntent(this))
        }

        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            connectionStateObserver.observe().collect {
                networkState.text = it.javaClass.simpleName
            }
        }
    }
}
