package piuk.blockchain.android.data.api.bitpay

import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.util.Locale

class BitPayDataManager constructor(
    private val bitPayService: BitPayService
) {

    /**
     * Returns a [RawPaymentRequest] object containing details about the BitPay invoice payment
     * request
     *
     * @param invoiceId The id of the BitPay invoice
     *
     * @return A [RawPaymentRequest] object wrapped in a [Single].
     */

    //TODO: create sep class
    fun getRawPaymentRequest(path: String = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE", invoiceId: String, currencyCode: String): Single<RawPaymentRequest> =
        bitPayService.getRawPaymentRequest(
            path = path,
            invoiceId = invoiceId,
            chain = currencyCode.toUpperCase(Locale.getDefault())
        ).applySchedulers()

    fun paymentVerificationRequest(path: String = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE", invoiceId: String,
                                   paymentRequest: BitPaymentRequest):
        Completable =
        bitPayService.getPaymentVerificationRequest(
            path = path,
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()

    fun paymentSubmitRequest(path: String = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE", invoiceId: String, paymentRequest: BitPaymentRequest):
        Completable =
        bitPayService.getPaymentSubmitRequest(
            path = path,
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()
}