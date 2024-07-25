package tech.relaycorp.gateway.test

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class TestLogHandler : Handler() {
    private val logs = mutableListOf<LogRecord>()

    override fun publish(record: LogRecord) {
        logs.add(record)
    }

    override fun flush() {}

    override fun close() {
        logs.clear()
    }

    fun filterLogs(level: Level, message: String): List<LogRecord> {
        return logs.filter { it.level == level && it.message == message }
    }
}
