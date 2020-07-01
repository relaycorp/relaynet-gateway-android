package tech.relaycorp.gateway.ui.main

import kotlinx.coroutines.channels.BroadcastChannel

fun <E> PublishFlow() = BroadcastChannel<E>(1)
