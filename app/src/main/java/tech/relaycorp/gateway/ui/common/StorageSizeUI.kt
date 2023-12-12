package tech.relaycorp.gateway.ui.common

import android.content.Context
import android.text.format.Formatter
import tech.relaycorp.gateway.data.model.StorageSize

fun StorageSize.format(context: Context): String = Formatter.formatFileSize(context, bytes)
