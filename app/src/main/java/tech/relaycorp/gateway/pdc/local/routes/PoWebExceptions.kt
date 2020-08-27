package tech.relaycorp.gateway.pdc.local.routes

abstract class PoWebException internal constructor(
    message: String?,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Base class for connectivity errors and errors caused by the server.
 */
abstract class ServerException internal constructor(message: String, cause: Throwable?) :
    PoWebException(message, cause)

/**
 * Error before or while connected to the server.
 *
 * The client should retry later.
 */
class ServerConnectionException(message: String, cause: Throwable? = null) :
    ServerException(message, cause)
