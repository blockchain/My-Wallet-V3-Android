package com.blockchain.network

import info.blockchain.balance.CryptoCurrency

interface EnvironmentUrls {
    val explorerUrl: String
    val apiUrl: String
    fun websocketUrl(currency: CryptoCurrency): String
}
