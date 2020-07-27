package tech.relaycorp.gateway.common

import java.time.ZoneId
import java.time.ZonedDateTime

fun nowInUtc() = ZonedDateTime.now(ZoneId.of("UTC"))
