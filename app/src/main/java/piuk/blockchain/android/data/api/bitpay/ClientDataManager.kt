package piuk.blockchain.android.data.api.bitpay

import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.util.Locale

abstract class ClientDataManager constructor(
    private val service: ClientService
) {

    /**
     * Returns a [RawPaymentRequest] object containing details about the BitPay invoice payment
     * request
     *
     * @param invoiceId The id of the BitPay invoice
     *
     * @return A [RawPaymentRequest] object wrapped in a [Single].
     */

    open fun getRawPaymentRequest(invoiceId: String, currencyCode: String): Single<RawPaymentRequest> =
        service.getRawPaymentRequest(
            invoiceId = invoiceId,
            chain = currencyCode.toUpperCase(Locale.getDefault())
        ).applySchedulers()

    open fun paymentVerificationRequest(invoiceId: String,
                                   paymentRequest: BitPaymentRequest):
        Completable =
        service.getPaymentVerificationRequest(
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()

    open fun paymentSubmitRequest(invoiceId: String, paymentRequest: BitPaymentRequest):
        Completable =
        service.getPaymentSubmitRequest(
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()
}