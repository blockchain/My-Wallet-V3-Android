package com.blockchain.morph.ui.homebrew.exchange

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.widget.TextViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.balance.layerListDrawableRes
import com.blockchain.morph.exchange.mvi.ApplyMaximumLimit
import com.blockchain.morph.exchange.mvi.ApplyMinimumLimit
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewState
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.Maximums
import com.blockchain.morph.exchange.mvi.SimpleFieldUpdateIntent
import com.blockchain.morph.exchange.mvi.SwapIntent
import com.blockchain.morph.exchange.mvi.ToggleFiatCryptoIntent
import com.blockchain.morph.exchange.mvi.isBase
import com.blockchain.morph.exchange.mvi.isCounter
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.ui.chooser.AccountChooserActivity
import com.blockchain.ui.chooser.AccountMode
import com.jakewharton.rxbinding2.view.clicks
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisibleIf
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

internal class ExchangeFragment : Fragment() {

    companion object {

        private const val ARGUMENT_CURRENCY = "ARGUMENT_CURRENCY"

        fun bundleArgs(fiatCurrency: String): Bundle = Bundle().apply {
            putString(ARGUMENT_CURRENCY, fiatCurrency)
        }
    }

    private val compositeDisposable = CompositeDisposable()
    private val activityListener: HomebrewHostActivityListener by ParentActivityDelegate(this)

    private lateinit var currency: String

    private lateinit var largeValueLeftHandSide: TextView
    private lateinit var largeValue: TextView
    private lateinit var largeValueCrypto: TextView
    private lateinit var largeValueRightHandSide: TextView
    private lateinit var smallValue: TextView
    private lateinit var keyboard: FloatKeyboardView
    private lateinit var selectSendAccountButton: Button
    private lateinit var selectReceiveAccountButton: Button
    private lateinit var exchangeButton: Button
    private lateinit var minButton: Button
    private lateinit var maxButton: Button
    private lateinit var largeValueFiatGroup: View

    private lateinit var exchangeModel: ExchangeModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val provider = (context as? ExchangeViewModelProvider)
            ?: throw Exception("Host activity must support ExchangeViewModelProvider")
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

        currency = arguments?.getString(ARGUMENT_CURRENCY) ?: "USD"

        largeValueLeftHandSide = view.findViewById(R.id.largeValueLeftHandSide)
        largeValueFiatGroup = view.findViewById(R.id.fiatLargeValueGroup)
        largeValue = view.findViewById(R.id.largeValue)
        largeValueCrypto = view.findViewById(R.id.largeValueCrypto)
        largeValueRightHandSide = view.findViewById(R.id.largeValueRightHandSide)
        smallValue = view.findViewById(R.id.smallValue)
        keyboard = view.findViewById(R.id.numericKeyboard)
        selectSendAccountButton = view.findViewById(R.id.select_from_account_button)
        selectReceiveAccountButton = view.findViewById(R.id.select_to_account_button)
        exchangeButton = view.findViewById(R.id.exchange_action_button)
        minButton = view.findViewById(R.id.minButton)
        maxButton = view.findViewById(R.id.maxButton)

        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            largeValueCrypto,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )

        selectSendAccountButton.setOnClickListener {
            AccountChooserActivity.startForResult(
                requireActivity(),
                AccountMode.Exchange,
                REQUEST_CODE_CHOOSE_SENDING_ACCOUNT,
                R.string.from
            )
        }
        selectReceiveAccountButton.setOnClickListener {
            AccountChooserActivity.startForResult(
                requireActivity(),
                AccountMode.Exchange,
                REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT,
                R.string.to
            )
        }
        exchangeButton.setOnClickListener {
            activityListener.launchConfirmation()
        }
    }

    override fun onResume() {
        super.onResume()

        keyboard.setMaximums(
            Maximums(
                maxDigits = 11,
                maxValue = 999_999.toBigDecimal()
            )
        )

        compositeDisposable +=
            Observable.merge(
                allTextUpdates(),
                clicksToIntents(R.id.imageview_switch_currency) { ToggleFiatCryptoIntent() },
                clicksToIntents(R.id.imageview_switch_from_to) { SwapIntent() },
                Observable.merge(
                    clicksToIntents(minButton) { ApplyMinimumLimit() },
                    clicksToIntents(maxButton) { ApplyMaximumLimit() }
                )
            ).subscribeBy {
                exchangeModel.inputEventSink.onNext(it)
            }

        val exchangeIndicator = view!!.findViewById<View>(R.id.imageView_exchange_indicator)
        val receiveIndicator = view!!.findViewById<View>(R.id.imageView_receive_indicator)
        compositeDisposable += exchangeModel
            .exchangeViewStates
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                updateMinAndMaxButtons(it)
                when (it.fix) {
                    Fix.BASE_FIAT -> displayFiatLarge(it.fromFiat, it.fromCrypto, it.decimalCursor)
                    Fix.BASE_CRYPTO -> displayCryptoLarge(it.fromCrypto, it.fromFiat, it.decimalCursor)
                    Fix.COUNTER_FIAT -> displayFiatLarge(it.toFiat, it.toCrypto, it.decimalCursor)
                    Fix.COUNTER_CRYPTO -> displayCryptoLarge(it.toCrypto, it.toFiat, it.decimalCursor)
                }
                selectSendAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.fromCrypto)
                selectReceiveAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.toCrypto)
                exchangeIndicator.invisibleIf(it.fix.isCounter)
                receiveIndicator.invisibleIf(it.fix.isBase)
                keyboard.setValue(it.lastUserValue.userDecimalPlaces, it.lastUserValue.toBigDecimal())
                exchangeButton.text = getString(
                    R.string.exchange_x_for_y,
                    it.fromAccount.cryptoCurrency.symbol,
                    it.toAccount.cryptoCurrency.symbol
                )
                exchangeButton.isEnabled = it.isValid()
            }
    }

    private fun updateMinAndMaxButtons(it: ExchangeViewState) {
        minButton.isEnabled = it.minTradeLimit != null
        maxButton.isEnabled = it.maxTradeLimit != null
    }

    private fun clicksToIntents(@IdRes id: Int, function: () -> ExchangeIntent) =
        clicksToIntents(view!!.findViewById<View>(id), function)

    private fun clicksToIntents(view: View, function: () -> ExchangeIntent) =
        view.clicks().map { function() }

    private fun displayFiatLarge(fiatValue: FiatValue, cryptoValue: CryptoValue, decimalCursor: Int) {
        val parts = fiatValue.toStringParts()
        largeValueLeftHandSide.text = parts.symbol
        largeValue.text = parts.major
        largeValueCrypto.text = ""
        largeValueRightHandSide.text = if (decimalCursor != 0) parts.minor else ""

        val fromCryptoString = cryptoValue.toStringWithSymbol()
        smallValue.text = fromCryptoString
    }

    @SuppressLint("SetTextI18n")
    private fun displayCryptoLarge(cryptoValue: CryptoValue, fiatValue: FiatValue, decimalCursor: Int) {
        largeValueLeftHandSide.text = ""
        largeValue.text = ""
        largeValueRightHandSide.text = ""
        largeValueRightHandSide.visibility = View.VISIBLE
        largeValueCrypto.text = cryptoValue.formatExactly(decimalCursor) + " " + cryptoValue.symbol()

        val fromFiatString = fiatValue.toStringWithSymbol()
        smallValue.text = fromFiatString
    }

    private fun allTextUpdates(): Observable<ExchangeIntent> {
        return keyboard.viewStates
            .doOnNext {
                if (it.shake) {
                    val animShake = AnimationUtils.loadAnimation(
                        requireContext(),
                        R.anim.fingerprint_failed_shake
                    )
                    largeValueFiatGroup.startAnimation(animShake)
                    largeValueCrypto.startAnimation(animShake)
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
            0 -> 0
            1 -> 1
            else -> decimalPlacesForCrypto - 1
        }
        return customCryptoEntryFormat
            .apply {
                minimumFractionDigits = show
                maximumFractionDigits = decimalPlacesForCrypto
            }.format(toMajorUnitDouble())
    }
}

private fun Money.formatOrSymbolForZero() =
    if (isZero) {
        symbol()
    } else {
        toStringWithSymbol()
    }

private fun Button.setButtonGraphicsAndTextFromCryptoValue(cryptoValue: CryptoValue) {
    val fromCryptoString = cryptoValue.formatOrSymbolForZero()
    setBackgroundResource(cryptoValue.currency.colorRes())
    setCryptoLeftImageIfZero(cryptoValue)
    text = fromCryptoString
}

private fun Button.setCryptoLeftImageIfZero(cryptoValue: CryptoValue) {
    if (cryptoValue.isZero) {
        setCompoundDrawablesWithIntrinsicBounds(
            context.getResolvedDrawable(
                cryptoValue.currency.layerListDrawableRes()
            ), null, null, null
        )
    } else {
        setCompoundDrawables(null, null, null, null)
    }
}

internal const val REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT = 800
internal const val REQUEST_CODE_CHOOSE_SENDING_ACCOUNT = 801
