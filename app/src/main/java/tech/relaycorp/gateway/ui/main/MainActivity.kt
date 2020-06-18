package tech.relaycorp.gateway.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tech.relaycorp.gateway.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.main_title)
        setContentView(R.layout.activity_main)
    }
}
