package tech.relaycorp.gateway.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.common_app_bar.*
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.ui.common.Insets.addSystemWindowInsetToPadding
import tech.relaycorp.gateway.ui.common.MessageManager

abstract class BaseActivity : AppCompatActivity() {

    private val app get() = applicationContext as App
    val component by lazy { app.component.activityComponent() }

    protected val messageManager by lazy { MessageManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup edge-to-edge UI
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        toolbarTitle?.text = title
        appBar?.addSystemWindowInsetToPadding(top = true)
    }

    protected fun setupNavigation(
        @DrawableRes icon: Int = R.drawable.ic_close,
        clickListener: (() -> Unit) = { finish() }
    ) {
        toolbar?.setNavigationIcon(icon)
        toolbar?.setNavigationOnClickListener { clickListener.invoke() }
    }
}
