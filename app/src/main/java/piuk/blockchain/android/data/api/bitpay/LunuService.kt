package piuk.blockchain.android.data.api.bitpay

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPayChain
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.exceptions.wrapErrorMessage
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Retrofit

class LunuService(
    environmentConfig: EnvironmentConfig,
    retrofit: Retrofit
) : ClientService(retrofit) {

    private val service: BitPay = retrofit.create(BitPay::class.java)
    private val baseUrl: String = environmentConfig.lunuUrl

    override fun getRawPaymentRequest(
        invoiceId: String,
        chain: String
    ): Single<RawPaymentRequest> =
        service.getRawPaymentRequest("$baseUrl$PATH_LUNU_INVOICE/$invoiceId", BitPayChain(chain))
            .wrapErrorMessage()

    override fun getPaymentVerificationRequest(
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable =
        service.paymentRequest(
            path = "$baseUrl$PATH_LUNU_INVOICE/$invoiceId",
            body = body,
            contentType = "application/payment-verification"
        )

    override fun getPaymentSubmitRequest(
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable = service.paymentRequest(
        path = "$baseUrl$PATH_LUNU_INVOICE/$invoiceId",
        body = body,
        contentType = "application/payment"
    )
}