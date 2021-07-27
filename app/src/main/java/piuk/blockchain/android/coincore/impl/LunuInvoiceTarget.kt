package piuk.blockchain.android.coincore.impl

import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoTarget
import piuk.blockchain.android.coincore.InvoiceTarget
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.LUNU_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import piuk.blockchain.android.data.api.bitpay.PATH_LUNU_INVOICE
import timber.log.Timber
import java.lang.IllegalStateException
import java.util.regex.Pattern

class LunuInvoiceTarget(
    override val asset: CryptoCurrency,
    override val address: String,
    val amount: CryptoValue,
    val invoiceId: String,
    val merchant: String,
    private val expires: String
) : InvoiceTarget, CryptoAddress {

    override val label: String = "BitPay[$merchant]"

    val expireTimeMs: Long by lazy {
        expires.fromIso8601ToUtc()?.toLocalTime()?.time ?: throw IllegalStateException("Unknown countdown time")
    }

    companion object {
        private const val INVOICE_PREFIX = "$LUNU_LIVE_BASE$PATH_LUNU_INVOICE"
        private val MERCHANT_PATTERN: Pattern = Pattern.compile("invoice ")

        fun fromLink(
            asset: CryptoCurrency,
            linkData: String,
            bitPayDataManager: BitPayDataManager
        ): Single<CryptoTarget> {
            val paymentRequestURL: String =
                FormatsUtil.getPaymentRequestUrl(linkData)

            var invoiceId: String = ""
            val idx = paymentRequestURL.lastIndexOf("/")
            if (idx != -1) {
                invoiceId = paymentRequestURL.substring(idx+1)
            }

            return bitPayDataManager.getRawPaymentRequest(path = INVOICE_PREFIX, invoiceId = invoiceId, currencyCode = asset.networkTicker)
                .map { rawRequest ->
                    LunuInvoiceTarget(
                        asset = asset,
                        amount = CryptoValue.fromMinor(asset, rawRequest.instructions[0].outputs[0].amount),
                        invoiceId = invoiceId,
                        merchant = rawRequest.memo.split(MERCHANT_PATTERN)[1],
                        address = rawRequest.instructions[0].outputs[0].address,
                        expires = rawRequest.expires
                    ) as CryptoTarget
                }.doOnError { e ->
                    Timber.e("Error loading invoice: $e")
                }
        }
    }
}