package tech.relaycorp.gateway.pdc

import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.poweb.PoWebClient

interface PoWebClientBuilder {
    suspend fun build(address: ServiceAddress): PoWebClient
}

interface PoWebClientProvider {
    suspend fun get(): PoWebClient
}
