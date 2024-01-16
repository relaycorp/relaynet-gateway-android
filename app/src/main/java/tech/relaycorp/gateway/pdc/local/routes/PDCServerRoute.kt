package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.server.routing.Routing

interface PDCServerRoute {
    fun register(routing: Routing)
}
