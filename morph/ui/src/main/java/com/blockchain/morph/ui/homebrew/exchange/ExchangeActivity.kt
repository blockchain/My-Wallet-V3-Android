package com.blockchain.morph.ui.homebrew.exchange

import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.FieldUpdateIntent
import com.blockchain.morph.exchange.mvi.initial
import com.blockchain.morph.ui.R
import com.blockchain.ui.chooser.AccountChooserActivity
import com.blockchain.ui.chooser.AccountMode
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.FormatPrecision
import info.blockchain.balance.formatWithUnit
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.Locale

class ExchangeActivityConfigurationChangePersistence : ViewModel() {

    var currentValue: Long = 0

    val from = CryptoCurrency.BTC
    val to = CryptoCurrency.ETHER

    var fieldMode = FieldUpdateIntent.Field.FROM_FIAT
}

// TODO: AND-1350 Unlikely to be needed long term. Added so that fake data isn't in the release code, only demo app.
interface RateStream {

    fun rateStream(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: String
    ): Observable<ExchangeIntent>
}

class ExchangeActivity : AppCompatActivity() {

    companion object {

        private var Currency = "CURRENCY"

        fun intent(context: Context, fiatCurrency: String) =
            Intent(context, ExchangeActivity::class.java).apply {
                putExtra(Currency, fiatCurrency)
            }
    }

    // TODO: AND-1350 Needed for Demo only, final implementation will just need a quote stream
    private val rateStream: RateStream by inject()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var configChangePersistence: ExchangeActivityConfigurationChangePersistence

    private lateinit var currency: String

    private lateinit var largeValueLeftHandSide: TextView
    private lateinit var largeValue: TextView
    private lateinit var largeValueRightHandSide: TextView
    private lateinit var smallValue: TextView
    private lateinit var keyboard: IntegerKeyboardView
    private lateinit var selectSendAccountButton: Button
    private lateinit var selectReceiveAccountButton: Button

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
    }

    override fun onResume() {
        super.onResume()
        val keyboard = findViewById<IntegerKeyboardView>(R.id.numericKeyboard)

        compositeDisposable += ExchangeDialog(
            Observable.merge(
                allTextUpdates(),
                rateStream.rateStream(
                    configChangePersistence.from,
                    configChangePersistence.to,
                    currency
                )
            ),
            initial(currency, configChangePersistence.from, configChangePersistence.to)
        ).viewModel
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d(it.toString())

                val parts = it.from.fiatValue.toParts(Locale.getDefault())
                largeValueLeftHandSide.text = parts.symbol
                largeValue.text = parts.major
                largeValueRightHandSide.text = parts.minor

                val fromCryptoString = it.from.cryptoValue.formatWithUnit(
                    Locale.getDefault(),
                    precision = FormatPrecision.Short
                )
                val toCryptoString = it.to.cryptoValue.formatWithUnit(
                    Locale.getDefault(),
                    precision = FormatPrecision.Short
                )
                smallValue.text = fromCryptoString
                selectSendAccountButton.text = fromCryptoString
                selectReceiveAccountButton.text = toCryptoString
            }
        keyboard.value = configChangePersistence.currentValue
    }

    private fun allTextUpdates(): Observable<ExchangeIntent> {
        return keyboard.valueChanges
            .doOnNext {
                configChangePersistence.currentValue = it
            }
            .map {
                FieldUpdateIntent(
                    configChangePersistence.fieldMode,
                    // TODO: AND-1363 This minor integer input could be an intent of its own. Certainly needs tests.
                    "",
                    when (configChangePersistence.fieldMode) {
                        FieldUpdateIntent.Field.FROM_FIAT,
                        FieldUpdateIntent.Field.TO_FIAT -> FiatValue.fromMinor(currency, it).value
                        FieldUpdateIntent.Field.FROM_CRYPTO -> CryptoValue(
                            configChangePersistence.from,
                            it.toBigInteger()
                        ).toMajorUnit()
                        FieldUpdateIntent.Field.TO_CRYPTO -> CryptoValue(
                            configChangePersistence.to,
                            it.toBigInteger()
                        ).toMajorUnit()
                    }
                )
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val account = AccountChooserActivity.getSelectedAccount(data)
            when (requestCode) {
                REQUEST_CODE_CHOOSE_SENDING_ACCOUNT ->
                    Timber.d("new ${account.cryptoCurrency} from account $account")
                REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT ->
                    Timber.d("new ${account.cryptoCurrency} to account $account")
                else -> throw IllegalArgumentException("Unknown request code $requestCode")
            }
        }
    }
}

private const val REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT = 800
private const val REQUEST_CODE_CHOOSE_SENDING_ACCOUNT = 801
