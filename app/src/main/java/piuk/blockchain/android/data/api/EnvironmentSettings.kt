package piuk.blockchain.android.data.api

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinCashTestNet3Params
import org.bitcoinj.params.BitcoinMainNetParams
import org.bitcoinj.params.BitcoinTestNet3Params
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.androidbuysell.api.COINIFY_LIVE_BASE
import piuk.blockchain.androidbuysell.api.COINIFY_SANDBOX_BASE
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class EnvironmentSettings : EnvironmentConfig {

    override fun websocketUrl(currency: CryptoCurrency) = when (currency) {
        CryptoCurrency.BTC -> BuildConfig.BITCOIN_WEBSOCKET_URL
        CryptoCurrency.ETHER -> BuildConfig.ETHEREUM_WEBSOCKET_URL
        CryptoCurrency.BCH -> BuildConfig.BITCOIN_CASH_WEBSOCKET_URL
    }

    override fun shouldShowDebugMenu(): Boolean = BuildConfig.DEBUG

    override val environment: Environment = Environment.fromString(BuildConfig.ENVIRONMENT)

    override val explorerUrl: String = BuildConfig.EXPLORER_URL

    override val apiUrl: String = BuildConfig.API_URL

    override val coinifyUrl: String
        get() = if (environment != Environment.PRODUCTION) COINIFY_SANDBOX_BASE else COINIFY_LIVE_BASE

    override val bitcoinNetworkParameters: NetworkParameters
        get() = when (environment) {
            Environment.TESTNET -> BitcoinTestNet3Params.get()
            else -> BitcoinMainNetParams.get()
        }

    override val bitcoinCashNetworkParameters: NetworkParameters
        get() = when (environment) {
            Environment.TESTNET -> BitcoinCashTestNet3Params.get()
            else -> BitcoinCashMainNetParams.get()
        }
}
