package tech.relaycorp.gateway.ui.common

import android.content.Intent

data class ActivityResult(
    val requestCode: Int,
    val resultCode: Int,
    val data: Intent?,
)
