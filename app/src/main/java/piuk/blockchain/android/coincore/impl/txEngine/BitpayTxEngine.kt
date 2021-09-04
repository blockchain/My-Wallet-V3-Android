package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.impl.BitPayInvoiceTarget
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager

class BitpayTxEngine(
    bitPayDataManager: BitPayDataManager,
    private val assetEngine: OnChainTxEngineBase,
    walletPrefs: WalletStatus,
    analytics: Analytics
) : ClientTxEngine(bitPayDataManager, assetEngine, walletPrefs, analytics) {

    override fun assertInputsValid() {
        // Only support non-custodial BTC & BCH bitpay at this time
        val supportedCryptoCurrencies = listOf(CryptoCurrency.BTC, CryptoCurrency.BCH)
        check(supportedCryptoCurrencies.contains(sourceAsset))
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is BitPayInvoiceTarget)
        require(assetEngine is PaymentClientEngine)
        assetEngine.assertInputsValid()
    }
}
