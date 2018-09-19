package piuk.blockchain.android.ui.buysell.confirmation.sell

import com.crashlytics.android.answers.PurchaseEvent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTradeRequest
import piuk.blockchain.androidbuysell.models.coinify.exceptions.CoinifyApiException
import piuk.blockchain.androidbuysell.services.ExchangeService
import com.blockchain.nabu.extensions.fromIso8601
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

class CoinifySellConfirmationPresenter @Inject constructor(
    private val coinifyDataManager: CoinifyDataManager,
    private val exchangeService: ExchangeService,
    private val payloadDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val stringUtils: StringUtils,
    private val environmentConfig: EnvironmentConfig
) : BasePresenter<CoinifySellConfirmationView>() {

    internal var validatedSecondPassword: String? = null

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    override fun onViewReady() {
        val expiryDateGmt = view.displayableQuote.originalQuote.expiryTime.fromIso8601()!!
        val calendar = Calendar.getInstance()
        val timeZone = calendar.timeZone
        val offset = timeZone.getOffset(expiryDateGmt.time)

        startCountdown(expiryDateGmt.time + offset)
    }

    internal fun onConfirmClicked() {
        if (payloadDataManager.isDoubleEncrypted && validatedSecondPassword == null) {
            view.displaySecondPasswordDialog()
            return
        }

        if (payloadDataManager.isDoubleEncrypted) {
            payloadDataManager.decryptHDWallet(
                environmentConfig.bitcoinNetworkParameters,
                validatedSecondPassword
            )
        }

        makeTransaction()
    }

    private fun makeTransaction() {
        val displayModel = view.displayableQuote
        val quote = displayModel.originalQuote
        val bankAccountId = view.bankAccountId
        val account = payloadDataManager.accounts[displayModel.accountIndex]

        tokenSingle
            .addToCompositeDisposable(this)
            .applySchedulers()
            .flatMap {
                coinifyDataManager.createNewTrade(
                    it,
                    CoinifyTradeRequest.sell(quote.id, bankAccountId)
                )
            }
            .flatMapObservable { trade ->
                sendDataManager.getUnspentOutputs(account.xpub)
                    .map {
                        sendDataManager.getSpendableCoins(
                            it,
                            displayModel.amountInSatoshis,
                            displayModel.feePerKb
                        )
                    }
                    .flatMap { spendable ->
                        payloadDataManager.getNextChangeAddress(account)
                            .flatMap {
                                sendDataManager.submitBtcPayment(
                                    spendable,
                                    payloadDataManager.getHDKeysForSigning(
                                        account,
                                        spendable
                                    ),
                                    (trade.transferIn.details as BlockchainDetails).account,
                                    it,
                                    displayModel.absoluteFeeInSatoshis,
                                    displayModel.amountInSatoshis
                                )
                            }
                    }
            }
            .doOnSubscribe { view.displayProgressDialog() }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onNext = {
                    Logging.logPurchase(
                        // Here we treat a sell event as purchasing fiat for BTC
                        PurchaseEvent()
                            .putCustomAttribute("currency", quote.quoteCurrency.toUpperCase())
                            .putItemPrice(quote.quoteAmount.absoluteValue.toBigDecimal())
                            .putItemName(quote.baseCurrency.toUpperCase())
                            .putItemType(Logging.ITEM_TYPE_FIAT)
                            .putSuccess(true)
                    )
                    view.showTransactionComplete()
                },
                onError = {
                    Timber.e(it)
                    Logging.logPurchase(
                        // Here we treat a sell event as purchasing fiat for BTC
                        PurchaseEvent()
                            .putCustomAttribute("currency", quote.quoteCurrency.toUpperCase())
                            .putItemPrice(quote.quoteAmount.absoluteValue.toBigDecimal())
                            .putItemName(quote.baseCurrency.toUpperCase())
                            .putItemType(Logging.ITEM_TYPE_FIAT)
                            .putSuccess(false)
                    )
                    if (it is CoinifyApiException) {
                        view.showErrorDialog(it.getErrorDescription())
                    } else {
                        view.showErrorDialog(stringUtils.getString(R.string.buy_sell_confirmation_unexpected_error))
                    }
                }
            )
    }

    private fun startCountdown(endTime: Long) {
        var remaining = (endTime - System.currentTimeMillis()) / 1000
        if (remaining <= 0) {
            // Finish page with error
            view.showQuoteExpiredDialog()
        } else {
            Observable.interval(1, TimeUnit.SECONDS)
                .addToCompositeDisposable(this)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEach { remaining-- }
                .map { return@map remaining }
                .doOnNext {
                    val readableTime = String.format(
                        "%2d:%02d",
                        TimeUnit.SECONDS.toMinutes(it),
                        TimeUnit.SECONDS.toSeconds(it) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(it))
                    )
                    view.updateCounter(readableTime)
                }
                .doOnNext { if (it < 5 * 60) view.showTimeExpiring() }
                .takeUntil { it <= 0 }
                .doOnComplete { view.showQuoteExpiredDialog() }
                .subscribe()
        }
    }
}
