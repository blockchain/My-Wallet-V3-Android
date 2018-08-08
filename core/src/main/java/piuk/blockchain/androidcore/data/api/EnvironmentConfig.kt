package piuk.blockchain.androidcore.data.api

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import org.bitcoinj.core.NetworkParameters

interface EnvironmentUrls {
    val explorerUrl: String
    val apiUrl: String
    fun websocketUrl(currency: CryptoCurrency): String
}

interface EnvironmentConfig : EnvironmentUrls {
    val environment: Environment
    val bitcoinNetworkParameters: NetworkParameters
    val bitcoinCashNetworkParameters: NetworkParameters

    val coinifyUrl: String

    fun shouldShowDebugMenu(): Boolean
}
