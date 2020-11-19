package tech.relaycorp.gateway.pdc

import tech.relaycorp.poweb.PoWebClient

interface PoWebClientBuilder {
    suspend fun build(): PoWebClient
}
