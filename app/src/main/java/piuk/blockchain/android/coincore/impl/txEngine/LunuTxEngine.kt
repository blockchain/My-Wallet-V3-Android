package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.bitcoinj.core.Transaction
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.LunuInvoiceTarget
import piuk.blockchain.android.data.api.bitpay.LunuDataManager
import piuk.blockchain.android.data.api.bitpay.models.BitPayTransaction
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest

class LunuTxEngine(
    private val lunuDataManager: LunuDataManager,
    private val assetEngine: OnChainTxEngineBase,
    walletPrefs: WalletStatus,
    analytics: Analytics
) : ClientTxEngine(assetEngine, walletPrefs, analytics) {

    override fun assertInputsValid() {
        // Only support non-custodial BTC lunu at this time
        val supportedCryptoCurrencies = listOf(CryptoCurrency.BTC)
        check(supportedCryptoCurrencies.contains(sourceAsset))
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is LunuInvoiceTarget)
        require(assetEngine is PaymentClientEngine)
        assetEngine.assertInputsValid()
    }

    override fun doVerifyTransaction(
        invoiceId: String,
        tx: Transaction
    ): Single<Transaction> =
        lunuDataManager.paymentVerificationRequest(
            invoiceId,
            BitPaymentRequest(
                sourceAsset.ticker,
                listOf(
                    BitPayTransaction(
                        String(Hex.encode(tx.bitcoinSerialize())),
                        tx.messageSize
                    )
                )
            )
        ).andThen(Single.just(tx))

    override fun doExecuteTransaction(
        invoiceId: String,
        tx: EngineTransaction
    ): Single<String> =
        lunuDataManager.paymentSubmitRequest(
            invoiceId,
            BitPaymentRequest(
                sourceAsset.ticker,
                listOf(
                    BitPayTransaction(
                        tx.encodedMsg,
                        tx.msgSize
                    )
                )
            )
        ).andThen(Single.just(tx.txHash))
}
