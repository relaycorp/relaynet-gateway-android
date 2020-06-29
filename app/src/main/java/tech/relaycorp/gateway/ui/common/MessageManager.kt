package tech.relaycorp.gateway.ui.common

import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject

class MessageManager
@Inject constructor(
    private val activity: Activity
) {

    private val resources get() = activity.resources
    private val rootView get() = activity.findViewById<View>(android.R.id.content)

    fun showMessage(@StringRes messageRes: Int) =
        showMessage(resources.getString(messageRes))

    fun showMessage(message: String) =
        showSnackbar(message)

    fun showError(@StringRes errorRes: Int) =
        showMessage(errorRes)

    private fun showSnackbar(text: String) =
        Snackbar
            .make(rootView, text, Snackbar.LENGTH_LONG)
            .show()
}
