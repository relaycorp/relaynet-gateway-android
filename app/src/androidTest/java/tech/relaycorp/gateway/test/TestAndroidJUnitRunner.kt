package tech.relaycorp.gateway.test

import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class TestAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?) =
        super.newApplication(cl, TestApp::class.java.name, context)!!
}
