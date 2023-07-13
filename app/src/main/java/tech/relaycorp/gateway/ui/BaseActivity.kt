package tech.relaycorp.gateway.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.common.ActivityResult
import tech.relaycorp.gateway.ui.common.Insets.addSystemWindowInsetToPadding
import tech.relaycorp.gateway.ui.common.MessageManager
import tech.relaycorp.gateway.ui.main.PublishFlow

abstract class BaseActivity : AppCompatActivity() {

    private val app get() = applicationContext as App
    val component by lazy { app.component.activityComponent() }

    protected val messageManager by lazy { MessageManager(this) }

    private val appBar get() = findViewById<AppBarLayout?>(R.id.appBar)
    private val toolbar get() = findViewById<Toolbar?>(R.id.toolbar)
    private val toolbarTitle get() = findViewById<TextView?>(R.id.toolbarTitle)

    protected val results get() = _results.asFlow()
    private val _results = PublishFlow<ActivityResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup edge-to-edge UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        toolbarTitle?.text = title
        appBar?.addSystemWindowInsetToPadding(top = true)
        findViewById<View>(R.id.innerContainer)?.addSystemWindowInsetToPadding(bottom = true)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        _results.trySendBlocking(ActivityResult(requestCode, resultCode, data))
    }

    protected fun setupNavigation(
        @DrawableRes icon: Int = R.drawable.ic_close,
        clickListener: (() -> Unit) = { finish() }
    ) {
        toolbar?.setNavigationIcon(icon)
        toolbar?.setNavigationOnClickListener { clickListener.invoke() }
    }
}
