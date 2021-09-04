package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.LunuInvoiceTarget
import piuk.blockchain.android.data.api.bitpay.LunuDataManager

class LunuTxEngine(
    lunuDataManager: LunuDataManager,
    private val assetEngine: OnChainTxEngineBase,
    walletPrefs: WalletStatus,
    analytics: Analytics
) : ClientTxEngine(lunuDataManager, assetEngine, walletPrefs, analytics) {

    override fun assertInputsValid() {
        // Only support non-custodial BTC lunu at this time
        val supportedCryptoCurrencies = listOf(CryptoCurrency.BTC)
        check(supportedCryptoCurrencies.contains(sourceAsset))
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is LunuInvoiceTarget)
        require(assetEngine is PaymentClientEngine)
        assetEngine.assertInputsValid()
    }
}
