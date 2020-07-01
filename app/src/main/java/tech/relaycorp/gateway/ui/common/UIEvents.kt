package tech.relaycorp.gateway.ui.common

import android.view.View

object Click
object Finish

enum class EnableState { Enabled, Disabled }

fun Boolean.toEnableState() = if (this) EnableState.Enabled else EnableState.Disabled

fun View.set(enableState: EnableState) {
    isEnabled = (enableState == EnableState.Enabled)
}
