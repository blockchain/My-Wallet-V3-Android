package piuk.blockchain.android.data.api.bitpay

import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPayChain
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.exceptions.wrapErrorMessage
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import retrofit2.Retrofit

class LunuPayService constructor(
    environmentConfig: EnvironmentConfig,
    retrofit: Retrofit,
    rxBus: RxBus
) {

    private val service: BitPay = retrofit.create(BitPay::class.java)
    private val rxPinning: RxPinning = RxPinning(rxBus)
    private val baseUrl: String = environmentConfig.bitpayUrl

    internal fun getRawPaymentRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        invoiceId: String,
        chain: String
    ): Single<RawPaymentRequest> = rxPinning.callSingle {
        service.getRawPaymentRequest("$path/$invoiceId", BitPayChain(chain))
            .wrapErrorMessage()
    }

    internal fun getPaymentVerificationRequest(
        path: String = "$baseUrl${PATH_LUNU_INVOICE}_INVOICE",
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