package piuk.blockchain.android.ui.swap.homebrew.exchange

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blockchain.logging.SwapDiagnostics
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.swap.common.exchange.mvi.ApplyMaxSpendable
import com.blockchain.swap.common.exchange.mvi.ExchangeIntent
import com.blockchain.swap.common.exchange.mvi.ExchangeViewState
import com.blockchain.swap.common.exchange.mvi.QuoteValidity
import com.blockchain.swap.common.exchange.mvi.SimpleFieldUpdateIntent
import com.blockchain.swap.common.exchange.mvi.ToggleFiatCryptoIntent
import com.blockchain.swap.nabu.service.Fix
import com.blockchain.swap.nabu.service.Quote
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.times
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_homebrew_exchange.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.swap.customviews.Maximums
import piuk.blockchain.android.ui.swap.customviews.ThreePartText
import piuk.blockchain.android.ui.swap.homebrew.exchange.host.HomebrewHostActivityListener
import piuk.blockchain.android.ui.swap.logging.AmountErrorType
import piuk.blockchain.android.ui.swap.logging.FixType
import piuk.blockchain.android.ui.swap.logging.amountErrorEvent
import piuk.blockchain.android.ui.swap.logging.fixTypeEvent
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.coinIconWhite
import piuk.blockchain.android.util.colorRes
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

internal class ExchangeFragment : Fragment() {

    companion object {

        private const val ARGUMENT_CURRENCY = "ARGUMENT_CURRENCY"
        fun bundleArgs(fiatCurrency: String): Bundle =
            Bundle().apply {
                putString(ARGUMENT_CURRENCY, fiatCurrency)
            }
    }

    private val compositeDisposable = CompositeDisposable()
    private val inputTypeRelay = PublishSubject.create<Fix>()
    private val activityListener: HomebrewHostActivityListener by ParentActivityDelegate(this)

    private lateinit var exchangeModel: ExchangeModel
    private lateinit var exchangeLimitState: ExchangeLimitState
    private lateinit var exchangeMenuState: ExchangeMenuState

    private var lastUserValue: Pair<Int, BigDecimal>? = null
    private var latestBaseFix: Fix = Fix.BASE_FIAT
    private var latestCryptoValue = CryptoValue.ZeroBtc
    private val stringUtils: StringUtils by inject()
    private val analytics: Analytics by inject()

    private val diagnostics: SwapDiagnostics by inject()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val provider = (context as? ExchangeViewModelProvider)
            ?: throw Exception("Host activity must support ExchangeViewModelProvider")
        exchangeLimitState = (context as? ExchangeLimitState)
            ?: throw Exception("Host activity must support ExchangeLimitState")
        exchangeMenuState = (context as? ExchangeMenuState)
            ?: throw Exception("Host activity must support ExchangeMenuState")
        exchangeModel = provider.exchangeViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_homebrew_exchange)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityListener.setToolbarTitle(R.string.morph_new_exchange)
        logEvent(AnalyticsEvents.ExchangeCreate)
        diagnostics.logIsMax(false)

        fragmentManager?.let { fragmentManager ->
            select_from_account_button.setOnClickListener {
                AccountChooserBottomDialog.create(
                    title = getString(R.string.dialog_title_exchange),
                    resultId = REQUEST_CODE_CHOOSE_SENDING_ACCOUNT
                ).show(fragmentManager, "BottomDialog")
                analytics.logEvent(SwapAnalyticsEvents.SwapLeftAssetClick)
            }

            select_to_account_button.setOnClickListener {
                AccountChooserBottomDialog.create(
                    title = getString(R.string.dialog_title_receive),
                    resultId = REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT
                ).show(fragmentManager, "BottomDialog")
                analytics.logEvent(SwapAnalyticsEvents.SwapRightAssetClick)
            }

            exchange_button.setOnClickListener {
                exchangeModel.fixAsCrypto()
                activityListener.launchConfirmation()
                analytics.logEvent(SwapAnalyticsEvents.SwapFormConfirmClick)
            }
        }

        large_value.setOnClickListener(toggleOnClickListener)
        small_value.setOnClickListener(toggleOnClickListener)

        balance_value.setOnClickListener {
            exchangeModel.inputEventSink.onNext(ApplyMaxSpendable)
            analytics.logEvent(SwapAnalyticsEvents.SwapMaxValueUsed)
            diagnostics.log("Max value clicked")
            diagnostics.logIsMax(true)
        }
    }

    private val toggleOnClickListener = View.OnClickListener {
        exchangeModel.inputEventSink.onNext(ToggleFiatCryptoIntent())
        analytics.logEvent(SwapAnalyticsEvents.SwapReversePairClick)
    }

    override fun onResume() {
        super.onResume()
        keyboard.setMaximums(Maximums(maxDigits = 11, maxIntLength = 6))

        compositeDisposable += allTextUpdates()
            .subscribeBy {
                exchangeModel.inputEventSink.onNext(it)
            }

        lastUserValue?.let { keyboard.setValue(it.first, it.second) }

        compositeDisposable += exchangeModel
            .exchangeViewStates
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it.fix) {
                    Fix.BASE_FIAT -> displayFiatLarge(it.fromFiat, it.fromCrypto, it.decimalCursor)
                    Fix.BASE_CRYPTO -> displayCryptoLarge(it.fromCrypto, it.fromFiat,
                        it.decimalCursor)
                    Fix.COUNTER_FIAT -> displayFiatLarge(it.toFiat, it.toCrypto, it.decimalCursor)
                    Fix.COUNTER_CRYPTO -> displayCryptoLarge(it.toCrypto, it.toFiat,
                        it.decimalCursor)
                }

                inputTypeRelay.onNext(it.fix)
                lastUserValue =
                    it.lastUserValue.userDecimalPlaces to it.lastUserValue.toBigDecimal()
                ExchangeCryptoButtonLayout(select_from_account_button,
                    select_from_account_text,
                    select_from_account_icon).setButtonGraphicsAndTextFromCryptoValue(it.fromCrypto)
                ExchangeCryptoButtonLayout(select_to_account_button,
                    select_to_account_text,
                    select_to_account_icon).setButtonGraphicsAndTextFromCryptoValue(it.toCrypto)

                if (latestCryptoValue != it.toCrypto && it.toCrypto.isZero.not()) {
                    analytics.logEvent(SwapAnalyticsEvents.SwapExchangeReceiveChange)
                }
                latestCryptoValue = it.toCrypto

                keyboard.setValue(it.lastUserValue.userDecimalPlaces,
                    it.lastUserValue.toBigDecimal())

                exchange_button.isEnabled = it.isValid()

                updateUserFeedBack(it)
                updateExchangeRate(it)
                updateBalance(it)
            }

        compositeDisposable += inputTypeRelay.map { it.toLoggingFixType() }
            .distinctUntilChanged()
            .subscribeBy {
                Logging.logEvent(fixTypeEvent(it))
            }

        compositeDisposable += exchangeModel
            .exchangeViewStates.distinctUntilChanged { prev, current -> prev.fix == current.fix }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                latestBaseFix = it.fix
            }

        if (latestBaseFix == Fix.BASE_CRYPTO) {
            exchangeModel.fixAsCrypto()
        } else if (latestBaseFix == Fix.BASE_FIAT) {
            exchangeModel.fixAsFiat()
        }
    }

    private fun updateBalance(viewState: ExchangeViewState) {
        balance_title.text =
            getString(R.string.morph_balance_title, viewState.fromCrypto.currencyCode)
        balance_value.text = viewState.formatSpendableString()
    }

    private fun updateExchangeRate(viewState: ExchangeViewState) {
        base_rate.text = viewState.formatBase()
        counter_rate.text = viewState.latestQuote?.let {
            viewState.formatCounterFromQuote(it)
        } ?: viewState.formatCounterFromPrices(viewState.exchangePrices)
    }

    private fun updateUserFeedBack(exchangeViewState: ExchangeViewState) {
        val error = exchangeViewState.exchangeError()

        // Set menu state
        val errorState = error?.let {
            ExchangeMenuState.ExchangeMenu.Error(it)
        } ?: ExchangeMenuState.ExchangeMenu.Help

        exchangeMenuState.setMenuState(errorState)
    }

    private fun displayFiatLarge(
        fiatValue: FiatValue,
        cryptoValue: CryptoValue,
        decimalCursor: Int
    ) {
        val parts = fiatValue.toStringParts()
        large_value.setText(
            ThreePartText(parts.symbol,
                parts.major,
                if (decimalCursor != 0) parts.minor else "")
        )

        val fromCryptoString = cryptoValue.toStringWithSymbol()
        small_value.text = fromCryptoString
    }

    @SuppressLint("SetTextI18n")
    private fun displayCryptoLarge(
        cryptoValue: CryptoValue,
        fiatValue: FiatValue,
        decimalCursor: Int
    ) {
        large_value.setText(
            ThreePartText("",
                cryptoValue.formatExactly(decimalCursor) + " " + cryptoValue.symbol,
                "")
        )

        val fromFiatString = fiatValue.toStringWithSymbol()
        small_value.text = fromFiatString
    }

    private fun allTextUpdates(): Observable<ExchangeIntent> {
        return keyboard.viewStates
            .doOnNext {
                if (it.shake) {
                    val animShake = AnimationUtils.loadAnimation(
                        requireContext(),
                        R.anim.fingerprint_failed_shake
                    )
                    large_value.startAnimation(animShake)
                }
                view!!.findViewById<View>(R.id.numberBackSpace).isEnabled = it.previous != null
            }
            .map {
                SimpleFieldUpdateIntent(it.userDecimal, it.decimalCursor)
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private val customCryptoEntryFormat: DecimalFormat =
        (NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat)

    private fun CryptoValue.formatExactly(decimalPlacesForCrypto: Int): String {
        val show = when (decimalPlacesForCrypto) {
            0, 1 -> decimalPlacesForCrypto
            else -> decimalPlacesForCrypto - 1
        }
        return customCryptoEntryFormat
            .apply {
                minimumFractionDigits = show
                maximumFractionDigits = decimalPlacesForCrypto
            }.format(toMajorUnitDouble())
    }

    private fun ExchangeViewState.exchangeError(): ExchangeMenuState.ExchangeMenuError? {

        val validity = validity()

        logMinMaxErrors(validity)

        exchangeLimitState.setOverTierLimit(validity == QuoteValidity.OverTierLimit)

        return when (validity) {
            QuoteValidity.Valid,
            QuoteValidity.NoQuote,
            QuoteValidity.MissMatch -> null
            QuoteValidity.NotEnoughFees -> {

                return ExchangeMenuState.ExchangeMenuError(
                    CryptoCurrency.ETHER,
                    userTier,
                    getString(R.string.pax_need_more_eth_error_title),
                    getString(R.string.erc20_need_more_eth_error_body_1, this.fromCrypto.currency.displayTicker),
                    ExchangeMenuState.ErrorType.TRADE
                )
            }
            QuoteValidity.UnderMinTrade -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.below_trading_limit),
                getString(R.string.under_min, minTradeLimit?.formatOrSymbolForZero()),
                ExchangeMenuState.ErrorType.TRADE
            )
            QuoteValidity.OverMaxTrade -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.above_trading_limit),
                getString(R.string.over_max, maxTradeLimit?.formatOrSymbolForZero()),
                ExchangeMenuState.ErrorType.TRADE
            )
            QuoteValidity.OverTierLimit -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.above_trading_limit),
                getString(R.string.over_max, maxTierLimit?.formatOrSymbolForZero()),
                ExchangeMenuState.ErrorType.TIER
            )
            QuoteValidity.OverUserBalance -> {
                val maxSpendableFiat = maxSpendableFiatBalance()
                ExchangeMenuState.ExchangeMenuError(
                    fromCrypto.currency,
                    userTier,
                    getString(R.string.not_enough_balance_for_coin,
                        fromCrypto.currency.displayTicker),
                    getString(
                        R.string.not_enough_balance,
                        maxSpendable?.toStringWithSymbol(),
                        maxSpendableFiat
                    ),
                    ExchangeMenuState.ErrorType.BALANCE
                )
            }
            QuoteValidity.HasTransactionInFlight -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.eth_in_flight_title),
                getString(R.string.eth_in_flight_msg, fromCrypto.currency.displayTicker),
                ExchangeMenuState.ErrorType.TRANSACTION_STATE
            )
        }
    }

    private fun logMinMaxErrors(validity: QuoteValidity) {
        val errorType = when (validity) {
            QuoteValidity.Valid,
            QuoteValidity.NotEnoughFees,
            QuoteValidity.NoQuote,
            QuoteValidity.HasTransactionInFlight,
            QuoteValidity.MissMatch -> null
            QuoteValidity.UnderMinTrade -> AmountErrorType.UnderMin
            QuoteValidity.OverMaxTrade -> AmountErrorType.OverMax
            QuoteValidity.OverTierLimit -> AmountErrorType.OverMax
            QuoteValidity.OverUserBalance -> AmountErrorType.OverBalance
        }

        errorType?.let { Logging.logEvent(amountErrorEvent(it)) }
    }

    private fun ExchangeViewState.formatSpendableString(): CharSequence {
        val cryptoCurrency = fromCrypto.currency
        val fiatCode = fromFiat.currencyCode
        val spendable = maxSpendable ?: CryptoValue.zero(cryptoCurrency)
        val spendableString = SpannableStringBuilder()
        try {
            val fiatSpendable = latestQuote?.baseToFiatRate?.let { baseToFiatRate ->
                ExchangeRate.CryptoToFiat(cryptoCurrency, fiatCode, baseToFiatRate)
                    .applyRate(spendable)
            } ?: spendable * c2fRate
            fiatSpendable?.let {
                val fiatString = SpannableString(it.toStringWithSymbol())
                fiatString.setSpan(
                    ForegroundColorSpan(getResolvedColor(R.color.product_green_medium)),
                    0,
                    fiatString.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spendableString.append(fiatString)
                spendableString.append(" ")
            }
            spendableString.append(spendable.toStringWithSymbol())
        } catch (e: Throwable) {
            Timber.e(
                "Race condition with new asset and spendable balance of the old asset ${e.message}"
            )
        }
        return spendableString
    }

    private fun ExchangeViewState.maxSpendableFiatBalance(): CharSequence {
        val cryptoCurrency = fromCrypto.currency
        val fiatCode = fromFiat.currencyCode
        val spendable = maxSpendable ?: CryptoValue.zero(cryptoCurrency)

        val fiat = latestQuote?.baseToFiatRate?.let { baseToFiatRate ->
            ExchangeRate.CryptoToFiat(cryptoCurrency, fiatCode, baseToFiatRate)
                .applyRate(spendable)
        } ?: FiatValue.zero(fiatCode)

        return fiat.toStringWithSymbol()
    }

    private fun ExchangeViewState.formatBase(): String =
        "1 ${fromCrypto.currencyCode} ="

    private fun ExchangeViewState.formatCounterFromQuote(quote: Quote): String =
        "${quote.baseToCounterRate} ${toCrypto.currencyCode}"

    private fun ExchangeViewState.formatCounterFromPrices(
        prices: List<ExchangeRate.CryptoToFiat>
    ): String {
        val exchangePriceFrom =
            prices.firstOrNull { it.from == fromCrypto.currency }?.rate ?: return ""
        val exchangePriceTo = prices.firstOrNull { it.from == toCrypto.currency }?.rate ?: return ""
        return "${exchangePriceFrom.divide(
            exchangePriceTo,
            toCrypto.currency.userDp,
            RoundingMode.HALF_DOWN)} ${toCrypto.currencyCode}"
    }

    private fun Fix.toLoggingFixType(): FixType = when (this) {
        Fix.BASE_FIAT -> FixType.BaseFiat
        Fix.BASE_CRYPTO -> FixType.BaseCrypto
        Fix.COUNTER_FIAT -> FixType.CounterFiat
        Fix.COUNTER_CRYPTO -> FixType.CounterCrypto
    }

    private fun ExchangeCryptoButtonLayout.setButtonGraphicsAndTextFromCryptoValue(
        cryptoValue: CryptoValue
    ) {
        val fromCryptoString = cryptoValue.formatOrSymbolForZero()
        button.setBackgroundResource(cryptoValue.currency.colorRes())

        textView.text = fromCryptoString
        imageView.setCryptoImageIfZero(cryptoValue)
        val params = textView.layoutParams as ConstraintLayout.LayoutParams
        if (cryptoValue.isZero)
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        else
            params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        textView.layoutParams = params
    }

    private fun ImageView.setCryptoImageIfZero(cryptoValue: CryptoValue) {
        if (cryptoValue.isZero) {
            val drawable =
                ContextCompat.getDrawable(activity ?: return, cryptoValue.currency.coinIconWhite())
            setImageDrawable(drawable)
        } else {
            setImageDrawable(null)
        }
    }

    class ExchangeCryptoButtonLayout(
        val button: Button,
        val textView: TextView,
        val imageView: ImageView
    )
}

internal const val REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT = 800
internal const val REQUEST_CODE_CHOOSE_SENDING_ACCOUNT = 801
