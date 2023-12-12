package tech.relaycorp.gateway.ui.main

import kotlinx.coroutines.flow.MutableSharedFlow

@Suppress("ktlint:standard:function-naming")
fun <E> PublishFlow() = MutableSharedFlow<E>(extraBufferCapacity = 1)
