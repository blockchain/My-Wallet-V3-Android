package piuk.blockchain.android.ui.buysell.confirmation.buy

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.createorder.models.BuyConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.createorder.models.OrderType
import piuk.blockchain.android.ui.buysell.details.models.AwaitingFundsModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.TradeData
import piuk.blockchain.androidbuysell.models.coinify.BankDetails
import piuk.blockchain.androidbuysell.models.coinify.CardDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTradeRequest
import piuk.blockchain.androidbuysell.models.coinify.exceptions.CoinifyApiException
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidbuysell.utils.fromIso8601
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CoinifyBuyConfirmationPresenter @Inject constructor(
    private val payloadDataManager: PayloadDataManager,
    private val coinifyDataManager: CoinifyDataManager,
    private val exchangeService: ExchangeService,
    private val stringUtils: StringUtils,
    private val metadataManager: MetadataManager
) : BasePresenter<CoinifyBuyConfirmationView>() {

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    override fun onViewReady() = Unit

    internal fun startCountdownTimer() {
        val expiryDateGmt = view.displayableQuote.originalQuote.expiryTime.fromIso8601()!!
        val calendar = Calendar.getInstance()
        val timeZone = calendar.timeZone
        val offset = timeZone.getOffset(expiryDateGmt.time)

        startCountdown(expiryDateGmt.time + offset)
    }

    internal fun onConfirmClicked() {
        val quote = view.displayableQuote

        when {
            view.orderType == OrderType.BuyCard -> completeCardBuy(quote)
            view.orderType == OrderType.BuyBank -> completeBankBuy(quote)
            else -> throw IllegalArgumentException("${view.orderType.name} not applicable to this page")
        }
    }

    internal fun onCardClicked() {
        val quote = view.displayableQuote
        if (quote.isHigherThanCardLimit) {
            view.showOverCardLimitDialog(quote.localisedCardLimit, quote.cardLimit)
        } else {
            view.launchCardConfirmation()
        }
    }

    internal fun onBankClicked() {
        view.launchBankConfirmation()
    }

    private fun completeCardBuy(quote: BuyConfirmationDisplayModel) {
        val addressPosition =
            payloadDataManager.getNextReceiveAddressPosition(payloadDataManager.accounts[quote.accountIndex])

        getAddressAndReserve(quote).flatMapSingle { address ->
            tokenSingle.flatMap {
                return@flatMap coinifyDataManager.createNewTrade(
                    it,
                    CoinifyTradeRequest.cardBuy(quote.originalQuote.id, address)
                )
            }
        }.singleOrError()
            .flatMap { trade ->
                updateMetadataCompletable(
                    addressPosition,
                    quote.accountIndex,
                    trade
                )
            }
            .doOnSubscribe { view.displayProgressDialog() }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    val cardDetails = it.transferIn.details as CardDetails
                    view.launchCardPaymentWebView(
                        cardDetails.redirectUrl,
                        cardDetails.paymentId,
                        it.inCurrency,
                        it.inAmount
                    )
                },
                onError = { handleException(it) }
            )
    }

    private fun completeBankBuy(quote: BuyConfirmationDisplayModel) {
        val addressPosition =
            payloadDataManager.getNextReceiveAddressPosition(payloadDataManager.accounts[quote.accountIndex])

        getAddressAndReserve(quote).flatMapSingle { address ->
            tokenSingle.flatMap {
                coinifyDataManager.createNewTrade(
                    it,
                    CoinifyTradeRequest.bankBuy(quote.originalQuote.id, address)
                )
            }
        }.singleOrError()
            .flatMap { trade ->
                updateMetadataCompletable(
                    addressPosition,
                    quote.accountIndex,
                    trade
                )
            }
            .doOnSubscribe { view.displayProgressDialog() }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    val model = getAwaitingFundsModel(it)
                    view.launchTransferDetailsPage(it.id, model)
                },
                onError = { handleException(it) }
            )
    }

    private fun updateMetadataCompletable(
        addressPosition: Int,
        accountIndex: Int,
        trade: CoinifyTrade
    ): Single<CoinifyTrade> = exchangeService.getExchangeMetaData()
        .map {
            if (it.coinify!!.trades == null) {
                it.coinify!!.trades = mutableListOf()
            }
            it.coinify!!.trades!!.add(
                TradeData()
                    .apply {
                        id = trade.id
                        state = trade.state.toString()
                        isBuy = false
                        this.accountIndex = accountIndex
                        receiveIndex = addressPosition
                        isConfirmed = false
                    }
            )

            return@map it
        }
        .flatMapCompletable {
            metadataManager.saveToMetadata(
                it.toSerialisedString(),
                ExchangeService.METADATA_TYPE_EXCHANGE
            )
        }
        .toSingle { trade }

    private fun getAddressAndReserve(quote: BuyConfirmationDisplayModel): Observable<String> =
        payloadDataManager.getNextReceiveAddressAndReserve(
            payloadDataManager.accounts[quote.accountIndex],
            stringUtils.getString(R.string.buy_sell_confirmation_order_id) + quote.originalQuote.id.toString()
        )

    private fun handleException(it: Throwable) {
        Timber.e(it)
        if (it is CoinifyApiException) {
            view.showErrorDialog(it.getErrorDescription())
        } else {
            view.showErrorDialog(stringUtils.getString(R.string.buy_sell_confirmation_unexpected_error))
        }
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

    private fun getAwaitingFundsModel(it: CoinifyTrade): AwaitingFundsModel {
        val (referenceText, account, bank, holder, _, _) = (it.transferIn.details as BankDetails)
        val formattedAmount = formatFiatWithSymbol(
            it.transferIn.sendAmount,
            it.transferIn.currency,
            view.locale
        )

        return AwaitingFundsModel(
            it.id,
            formattedAmount,
            referenceText,
            holder.name,
            holder.address.getFormattedAddressString(),
            account.number,
            account.bic,
            "${bank.name}, ${bank.address.getFormattedAddressString()}"
        )
    }

    private fun formatFiatWithSymbol(
        fiatValue: Double,
        currencyCode: String,
        locale: Locale
    ): String {
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        val decimalFormatSymbols = (numberFormat as DecimalFormat).decimalFormatSymbols
        numberFormat.decimalFormatSymbols = decimalFormatSymbols.apply {
            this.currencySymbol = Currency.getInstance(currencyCode).getSymbol(locale)
        }
        return numberFormat.format(fiatValue)
    }
}