package com.blockchain.logging

/**
 * Loggers like Android's and Timber depend on Android, so are unsuitable for plain Kotlin modules.
 * Use Timber where possible, but where not, inject and use [Logger].
 */
interface Logger {
    fun d(s: String)
    fun v(s: String)
    fun e(s: String)
}

object NullLogger : Logger {
    override fun d(s: String) {}
    override fun v(s: String) {}
    override fun e(s: String) {}
}
