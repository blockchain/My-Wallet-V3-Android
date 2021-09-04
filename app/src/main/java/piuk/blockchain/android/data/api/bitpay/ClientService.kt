package piuk.blockchain.android.data.api.bitpay

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import retrofit2.Retrofit

abstract class ClientService constructor(retrofit: Retrofit) {
    abstract fun getRawPaymentRequest(
        invoiceId: String,
        chain: String
    ): Single<RawPaymentRequest>

    abstract fun getPaymentVerificationRequest(
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable

    abstract fun getPaymentSubmitRequest(
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable
}