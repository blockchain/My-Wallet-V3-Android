package piuk.blockchain.android.data.api.bitpay

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPayChain
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.exceptions.wrapErrorMessage
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import retrofit2.Retrofit

class LunuService constructor(
    environmentConfig: EnvironmentConfig,
    retrofit: Retrofit,
) {

    private val service: BitPay = retrofit.create(BitPay::class.java)
    private val baseUrl: String = environmentConfig.lunuUrl

    internal fun getRawPaymentRequest(
        path: String = "$baseUrl$PATH_LUNU_INVOICE",
        invoiceId: String,
        chain: String
    ): Single<RawPaymentRequest> =
        service.getRawPaymentRequest("$path/$invoiceId", BitPayChain(chain))
            .wrapErrorMessage()

    internal fun getPaymentVerificationRequest(
        path: String = "$baseUrl$PATH_LUNU_INVOICE",
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable =
        service.paymentRequest(path = "$path/$invoiceId",
            body = body,
            contentType = "application/payment-verification")

    internal fun getPaymentSubmitRequest(
        path: String = "$baseUrl$PATH_LUNU_INVOICE",
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable = service.paymentRequest(path = "$path/$invoiceId",
        body = body,
        contentType = "application/payment")
}