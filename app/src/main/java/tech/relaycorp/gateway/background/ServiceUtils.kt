package tech.relaycorp.gateway.background

import android.app.Service
import tech.relaycorp.gateway.App

val Service.component get() = (applicationContext as App).component
