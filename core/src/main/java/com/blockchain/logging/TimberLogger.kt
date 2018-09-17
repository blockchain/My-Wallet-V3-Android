package com.blockchain.logging

import timber.log.Timber

internal class TimberLogger : Logger {

    override fun d(s: String) {
        Timber.d(s)
    }

    override fun v(s: String) {
        Timber.v(s)
    }

    override fun e(s: String) {
        Timber.e(s)
    }
}
