package com.blockchain.morph.ui.homebrew.exchange

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.balance.layerListDrawableRes
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.SimpleFieldUpdateIntent
import com.blockchain.morph.exchange.mvi.ToggleFiatCryptoIntent
import com.blockchain.morph.exchange.mvi.ToggleFromToIntent
import com.blockchain.morph.exchange.mvi.Value
import com.blockchain.morph.exchange.mvi.fixedField
import com.blockchain.morph.exchange.mvi.isBase
import com.blockchain.morph.exchange.mvi.isCounter
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.ui.chooser.AccountChooserActivity
import com.blockchain.ui.chooser.AccountMode
import com.jakewharton.rxbinding2.view.clicks
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FormatPrecision
import info.blockchain.balance.formatWithUnit
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisibleIf
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

    private lateinit var configChangePersistence: ExchangeFragmentConfigurationChangePersistence

    private lateinit var currency: String

    private lateinit var largeValueLeftHandSide: TextView
    private lateinit var largeValue: TextView
    private lateinit var largeValueRightHandSide: TextView
    private lateinit var smallValue: TextView
    private lateinit var keyboard: FloatKeyboardView
    private lateinit var selectSendAccountButton: Button
    private lateinit var selectReceiveAccountButton: Button
    private lateinit var exchangeButton: Button

    private lateinit var exchangeModel: ExchangeModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val provider = (context as? ExchangeViewModelProvider)
            ?: throw Exception("Host activity must support ExchangeViewModelProvider")
        exchangeModel = provider.exchangeViewModel
        configChangePersistence = exchangeModel.configChangePersistence
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
        largeValue = view.findViewById(R.id.largeValue)
        largeValueRightHandSide = view.findViewById(R.id.largeValueRightHandSide)
        smallValue = view.findViewById(R.id.smallValue)
        keyboard = view.findViewById(R.id.numericKeyboard)
        selectSendAccountButton = view.findViewById(R.id.select_from_account_button)
        selectReceiveAccountButton = view.findViewById(R.id.select_to_account_button)
        exchangeButton = view.findViewById(R.id.exchange_action_button)

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

        compositeDisposable +=
            Observable.merge(
                allTextUpdates(),
                view!!.findViewById<View>(R.id.imageview_switch_currency).clicks().map { ToggleFiatCryptoIntent() },
                view!!.findViewById<View>(R.id.imageview_switch_from_to).clicks().map { ToggleFromToIntent() }
            ).subscribeBy {
                exchangeModel.inputEventSink.onNext(it)
            }

        val exchangeIndicator = view!!.findViewById<View>(R.id.imageView_exchange_indicator)
        val receiveIndicator = view!!.findViewById<View>(R.id.imageView_receive_indicator)
        compositeDisposable += exchangeModel
            .exchangeViewModels
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it.fixedField) {
                    Fix.BASE_FIAT -> displayFiatLarge(it.from)
                    Fix.BASE_CRYPTO -> displayCryptoLarge(it.from)
                    Fix.COUNTER_FIAT -> displayFiatLarge(it.to)
                    Fix.COUNTER_CRYPTO -> displayCryptoLarge(it.to)
                }
                selectSendAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.from)
                selectReceiveAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.to)
                exchangeIndicator.invisibleIf(it.fixedField.isCounter)
                receiveIndicator.invisibleIf(it.fixedField.isBase)
            }
    }

    private fun displayFiatLarge(from: Value) {
        val parts = from.fiatValue.toParts(Locale.getDefault())
        largeValueLeftHandSide.text = parts.symbol
        largeValue.text = parts.major
        largeValueRightHandSide.text = parts.minor

        val fromCryptoString = from.cryptoValue.formatForExchange()
        smallValue.text = fromCryptoString
    }

    private fun displayCryptoLarge(from: Value) {
        largeValueLeftHandSide.text = ""
        largeValue.text = from.cryptoValue.formatForExchange()
        largeValueRightHandSide.text = ""

        val fromFiatString = from.fiatValue.toStringWithSymbol(Locale.getDefault())
        smallValue.text = fromFiatString
    }

    private fun allTextUpdates(): Observable<ExchangeIntent> {
        return keyboard.viewStates
            .doOnNext {
                configChangePersistence.currentValue = it.userDecimal
                if (it.shake) {
                    val animShake = AnimationUtils.loadAnimation(
                        requireContext(),
                        R.anim.fingerprint_failed_shake
                    )
                    largeValue.startAnimation(animShake)
                    largeValueRightHandSide.startAnimation(animShake)
                    largeValueLeftHandSide.startAnimation(animShake)
                }
                largeValueRightHandSide.invisibleIf(it.decimalCursor == 0)
                view!!.findViewById<View>(R.id.numberBackSpace).isEnabled = it.previous != null
            }
            .map { it.userDecimal }
            .distinctUntilChanged()
            .map {
                SimpleFieldUpdateIntent(it)
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}

private fun CryptoValue.formatOrSymbolForZero() =
    if (isZero) {
        currency.symbol
    } else {
        formatForExchange()
    }

private fun CryptoValue.formatForExchange() =
    formatWithUnit(
        Locale.getDefault(),
        precision = FormatPrecision.Short
    )

private fun Button.setButtonGraphicsAndTextFromCryptoValue(
    from: Value
) {
    val fromCryptoString = from.cryptoValue.formatOrSymbolForZero()
    setBackgroundResource(from.cryptoValue.currency.colorRes())
    setCryptoLeftImageIfZero(from.cryptoValue)
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
