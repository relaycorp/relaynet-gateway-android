package tech.relaycorp.gateway.ui.main

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.sync.CourierConnectionActivity

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.main_title)
        setContentView(R.layout.activity_main)

        syncCourier.setOnClickListener {
            startActivity(CourierConnectionActivity.getIntent(this))
        }
    }
}
