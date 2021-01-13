package tech.relaycorp.gateway.ui.common

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

fun Context.getColorFromAttr(@AttrRes colorAttr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(colorAttr, typedValue, true)
    return typedValue.data
}
