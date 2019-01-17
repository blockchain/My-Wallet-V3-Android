package com.blockchain.notifications.analytics

import org.koin.android.ext.android.get

fun android.content.ComponentCallbacks.logEvent(loggable: Loggable) {
    get<EventLogger>().logEvent(loggable)
}
