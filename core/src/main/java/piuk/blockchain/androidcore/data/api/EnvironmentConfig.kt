package piuk.blockchain.androidcore.data.api

import com.blockchain.network.EnvironmentUrls
import info.blockchain.wallet.api.Environment
import org.bitcoinj.core.NetworkParameters

interface EnvironmentConfig : EnvironmentUrls {
    val environment: Environment
    val bitcoinNetworkParameters: NetworkParameters
    val bitcoinCashNetworkParameters: NetworkParameters

    val coinifyUrl: String
    val appVersion: String

    fun shouldShowDebugMenu(): Boolean
}
