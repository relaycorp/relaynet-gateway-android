package tech.relaycorp.gateway.common

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

fun Date.toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(toInstant(), ZoneId.of("UTC"))
fun ZonedDateTime.toDate(): Date = Date.from(toInstant())
