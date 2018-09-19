package com.blockchain.koin.modules

import com.blockchain.features.FeatureNames
import piuk.blockchain.android.BuildConfig

val features = mapOf(
    FeatureNames.CONTACTS to BuildConfig.CONTACTS_ENABLED
)

val appProperties = listOf(
    "app-version" to BuildConfig.VERSION_NAME
)

val keys = listOf(
    "api-code" to "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3"
)

val webSocketUrls = listOf(
    WebSocketUrlKeys.Nabu to BuildConfig.NABU_WEBSOCKET_URL
)

object WebSocketUrlKeys {
    const val Nabu: String = "nabu-webSocket-url"
}
