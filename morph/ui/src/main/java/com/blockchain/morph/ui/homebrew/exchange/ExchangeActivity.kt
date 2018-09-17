package com.blockchain.morph.ui.homebrew.exchange

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.balance.layerListDrawableRes
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.FieldUpdateIntent
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.mvi.Value
import com.blockchain.morph.exchange.mvi.initial
import com.blockchain.morph.exchange.mvi.toIntent
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.homebrew.QuoteWebSocket
import com.blockchain.morph.homebrew.authenticate
import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.morph.ui.R
import com.blockchain.ui.chooser.AccountChooserActivity
import com.blockchain.ui.chooser.AccountMode
import com.blockchain.nabu.Authenticator
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.network.websocket.openAsDisposable
import com.squareup.moshi.Moshi
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.FormatPrecision
import info.blockchain.balance.formatWithUnit
import info.blockchain.balance.withMajorValue
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import piuk.blockchain.androidcoreui.utils.extensions.invisibleIf
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale

// TODO: AND-1363 This class has too much in it. Need to extract and place in
// :morph:homebrew with interfaces in :morph:common
class ExchangeActivity : BaseAuthActivity() {

    companion object {

        private var Currency = "CURRENCY"

        fun intent(context: Context, fiatCurrency: String) =
            Intent(context, ExchangeActivity::class.java).apply {
                putExtra(Currency, fiatCurrency)
            }
    }

    private val compositeDisposable = CompositeDisposable()
    private val dialogDisposable = CompositeDisposable()

    private lateinit var configChangePersistence: ExchangeActivityConfigurationChangePersistence

    private lateinit var currency: String

    private lateinit var largeValueLeftHandSide: TextView
    private lateinit var largeValue: TextView
    private lateinit var largeValueRightHandSide: TextView
    private lateinit var smallValue: TextView
    private lateinit var keyboard: FloatKeyboardView
    private lateinit var selectSendAccountButton: Button
    private lateinit var selectReceiveAccountButton: Button
    private lateinit var exchangeButton: Button

    private var quoteWebSocket: QuoteWebSocket? = null

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_exchange)

        configChangePersistence = ViewModelProviders.of(this)
            .get(ExchangeActivityConfigurationChangePersistence::class.java)

        currency = intent.getStringExtra(Currency) ?: "USD"

        largeValueLeftHandSide = findViewById(R.id.largeValueLeftHandSide)
        largeValue = findViewById(R.id.largeValue)
        largeValueRightHandSide = findViewById(R.id.largeValueRightHandSide)
        smallValue = findViewById(R.id.smallValue)
        keyboard = findViewById(R.id.numericKeyboard)
        selectSendAccountButton = findViewById(R.id.select_from_account_button)
        selectReceiveAccountButton = findViewById(R.id.select_to_account_button)
        exchangeButton = findViewById(R.id.exchange_action_button)

        selectSendAccountButton.setOnClickListener {
            AccountChooserActivity.startForResult(
                this@ExchangeActivity,
                AccountMode.Exchange,
                REQUEST_CODE_CHOOSE_SENDING_ACCOUNT,
                R.string.from
            )
        }
        selectReceiveAccountButton.setOnClickListener {
            AccountChooserActivity.startForResult(
                this@ExchangeActivity,
                AccountMode.Exchange,
                REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT,
                R.string.to
            )
        }
        // TODO: Temporary to see the full exchange flow UI
        exchangeButton.setOnClickListener {
            val intent = Intent(this, ExchangeConfirmationActivity::class.java)
            startActivity(intent)
        }
        setupToolbar(R.id.toolbar_constraint, R.string.morph_new_exchange)
    }

    override fun onResume() {
        super.onResume()
        updateMviDialog()
    }

    private fun newQuoteWebSocket(): QuoteWebSocket {
        val auth = get<Authenticator>()
        val moshi = get<Moshi>()

        val socket = get<OkHttpClient>().newBlockchainWebSocket(
            Options(
                url = "wss://ws.dev.blockchain.info/nabu-gateway/markets/quotes",
                origin = "https://blockchain.info"
            )
        )
            .timber("Quotes")
            .autoRetry()
            .authenticate(auth)

        val quotesSocket = QuoteWebSocket(socket, moshi)

        compositeDisposable += quotesSocket.connectionStatus
            .map {
                it == QuoteService.Status.Open
            }
            .distinctUntilChanged()
            .subscribe {
                if (it) {
                    snackbar?.dismiss()
                } else {
                    snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.connection_error,
                        Snackbar.LENGTH_INDEFINITE
                    ).apply {
                        show()
                    }
                }
            }

        compositeDisposable += socket.openAsDisposable()

        quoteWebSocket = quotesSocket
        return quotesSocket
    }

    private fun updateMviDialog() {
        dialogDisposable.clear()
        val quotesSocket = newQuoteWebSocket()

        compositeDisposable += quotesSocket.quotes
            .subscribeBy {
                Timber.d("Quote: $it")
            }

        dialogDisposable += ExchangeDialog(
            Observable.merge(
                allTextUpdates(quotesSocket),
                quotesSocket.quotes.map(Quote::toIntent)
            ),
            initial(currency, configChangePersistence.from, configChangePersistence.to)
        ).viewModel
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.e(it) }
            .subscribeBy {

                Timber.d(it.toString())

                val parts = it.from.fiatValue.toParts(Locale.getDefault())
                largeValueLeftHandSide.text = parts.symbol
                largeValue.text = parts.major
                largeValueRightHandSide.text = parts.minor

                val fromCryptoString = it.from.cryptoValue.formatForExchange()
                smallValue.text = fromCryptoString
                selectSendAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.from)
                selectReceiveAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.to)
            }
    }

    private fun allTextUpdates(quotesSocket: QuoteWebSocket): Observable<ExchangeIntent> {
        return keyboard.viewStates
            .doOnNext {
                configChangePersistence.currentValue = it.userDecimal
                if (it.shake) {
                    val animShake = AnimationUtils.loadAnimation(this, R.anim.fingerprint_failed_shake)
                    largeValue.startAnimation(animShake)
                    largeValueRightHandSide.startAnimation(animShake)
                    largeValueLeftHandSide.startAnimation(animShake)
                }
                largeValueRightHandSide.invisibleIf(it.decimalCursor == 0)
                findViewById<View>(R.id.numberBackSpace).isEnabled = it.previous != null
            }
            .map { it.userDecimal }
            .doOnNext {
                quotesSocket.updateQuoteRequest(it.toExchangeQuoteRequest(configChangePersistence, currency))
            }
            .distinctUntilChanged()
            .map {
                FieldUpdateIntent(
                    configChangePersistence.fieldMode,
                    // TODO: AND-1363 This minor integer input could be an intent of its own. Certainly needs tests.
                    "",
                    it
                )
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        dialogDisposable.clear()
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val account = AccountChooserActivity.getSelectedAccount(data)
            when (requestCode) {
                REQUEST_CODE_CHOOSE_SENDING_ACCOUNT -> {
                    configChangePersistence.from = account.cryptoCurrency
                    updateMviDialog()
                }
                REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT -> {
                    configChangePersistence.to = account.cryptoCurrency
                    updateMviDialog()
                }
                else -> throw IllegalArgumentException("Unknown request code $requestCode")
            }
        }
    }
}

private fun BigDecimal.toExchangeQuoteRequest(
    field: ExchangeActivityConfigurationChangePersistence,
    currency: String
): ExchangeQuoteRequest {
    return when (field.fieldMode) {
        FieldUpdateIntent.Field.TO_FIAT ->
            ExchangeQuoteRequest.BuyingFiatLinked(
                offering = field.from,
                wanted = field.to,
                wantedFiatValue = FiatValue.fromMajor(currency, this)
            )
        FieldUpdateIntent.Field.FROM_FIAT ->
            ExchangeQuoteRequest.SellingFiatLinked(
                offering = field.from,
                wanted = field.to,
                offeringFiatValue = FiatValue.fromMajor(currency, this)
            )
        FieldUpdateIntent.Field.TO_CRYPTO ->
            ExchangeQuoteRequest.Buying(
                offering = field.from,
                wanted = field.to.withMajorValue(this),
                indicativeFiatSymbol = currency
            )
        FieldUpdateIntent.Field.FROM_CRYPTO ->
            ExchangeQuoteRequest.Selling(
                offering = field.from.withMajorValue(this),
                wanted = field.to,
                indicativeFiatSymbol = currency
            )
    }
}

private fun CryptoValue.formatOrSymbolForZero() =
    if (isZero()) {
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
    if (cryptoValue.isZero()) {
        setCompoundDrawablesWithIntrinsicBounds(
            context.getResolvedDrawable(
                cryptoValue.currency.layerListDrawableRes()
            ), null, null, null
        )
    } else {
        setCompoundDrawables(null, null, null, null)
    }
}

private const val REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT = 800
private const val REQUEST_CODE_CHOOSE_SENDING_ACCOUNT = 801
