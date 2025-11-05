package no.nav.aap.api.util

fun Throwable.findRootCause(): Throwable {
    var current = this
    var cause: Throwable? = current.cause
    while (cause != null && cause != current) { // Prevent infinite loops in case of circular causes
        current = cause
        cause = current.cause
    }
    return current
}