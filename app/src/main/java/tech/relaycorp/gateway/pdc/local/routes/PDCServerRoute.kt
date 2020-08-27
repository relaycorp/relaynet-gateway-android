package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.routing.Routing

interface PDCServerRoute {
    fun register(routing: Routing)
}
