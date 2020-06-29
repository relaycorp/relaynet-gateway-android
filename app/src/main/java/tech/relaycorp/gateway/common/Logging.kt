package tech.relaycorp.gateway.common

import java.util.logging.Level
import java.util.logging.Logger

object Logging {

    private val rootLogger by lazy { Logger.getLogger("") }

    val Any.logger: Logger get() = Logger.getLogger(javaClass.name)

    var level: Level
        get() = rootLogger.level ?: Level.ALL
        set(value) {
            rootLogger.level = value
        }
}
