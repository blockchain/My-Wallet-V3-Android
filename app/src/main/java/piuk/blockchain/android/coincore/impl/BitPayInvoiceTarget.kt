package piuk.blockchain.android.coincore.impl

import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoTarget
import piuk.blockchain.android.coincore.InvoiceTarget
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import timber.log.Timber
import java.lang.IllegalStateException
import java.util.regex.Pattern

class BitPayInvoiceTarget(
    override val asset: AssetInfo,
    override val address: String,
    override val amount: CryptoValue,
    override val invoiceId: String,
    override val merchant: String,
    override val expires: String
) : InvoiceTarget {

    override val label: String = "BitPay[$merchant]"

    override val expireTimeMs: Long by lazy {
        expires.fromIso8601ToUtc()?.toLocalTime()?.time ?: throw IllegalStateException("Unknown countdown time")
    }

    companion object {
        private const val INVOICE_PREFIX = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE/"
        private val MERCHANT_PATTERN: Pattern = Pattern.compile("for merchant ")

        fun fromLink(
            asset: AssetInfo,
            linkData: String,
            bitPayDataManager: BitPayDataManager
        ): Single<CryptoTarget> {
            val invoiceId: String =
                FormatsUtil.getPaymentRequestUrl(linkData)
                    .replace(INVOICE_PREFIX, "")

            return bitPayDataManager.getRawPaymentRequest(invoiceId = invoiceId, currencyCode = asset.ticker)
                .map { rawRequest ->
                    BitPayInvoiceTarget(
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