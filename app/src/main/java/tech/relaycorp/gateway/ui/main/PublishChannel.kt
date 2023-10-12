package tech.relaycorp.gateway.ui.main

import kotlinx.coroutines.flow.MutableSharedFlow

fun <E> PublishFlow() = MutableSharedFlow<E>(extraBufferCapacity = 1)
