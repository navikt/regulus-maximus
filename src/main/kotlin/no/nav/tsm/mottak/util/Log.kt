package no.nav.tsm.mottak.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.java

inline fun <reified T> T.applog(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}
inline fun <reified T> T.teamLogger(): Logger =
    LoggerFactory.getLogger("teamlog.${T::class.java.name}")
