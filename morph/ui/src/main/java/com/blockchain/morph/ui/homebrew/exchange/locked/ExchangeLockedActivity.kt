package com.blockchain.morph.ui.homebrew.exchange.locked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryActivity
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.parcel.Parcelize
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ExchangeLockedActivity : BaseAuthActivity() {

    private val fromButton by unsafeLazy { findViewById<Button>(R.id.select_from_account_button) }
    private val toButton by unsafeLazy { findViewById<Button>(R.id.select_to_account_button) }
    private val doneButton by unsafeLazy { findViewById<Button>(R.id.button_done) }
    private val orderIdTextView by unsafeLazy { findViewById<TextView>(R.id.order_id_textView) }
    private val valueTextView by unsafeLazy { findViewById<TextView>(R.id.value_textView) }
    private val feesTextView by unsafeLazy { findViewById<TextView>(R.id.fees_textView) }
    private val receiveTextView by unsafeLazy { findViewById<TextView>(R.id.receive_textView) }
    private val sendToTextView by unsafeLazy { findViewById<TextView>(R.id.send_to_textView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_locked)

        setupToolbar(R.id.toolbar_general, R.string.exchange_locked)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        renderUi(intent.getParcelableExtra(EXTRA_CONFIRMATION_MODEL))

        doneButton.setOnClickListener {
            val intent = Intent(this, TradeHistoryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean =
        consume { menuInflater.inflate(R.menu.menu_exchange_locked, menu) }

    private fun renderUi(model: ExchangeLockedModel) {
        with(model) {
            orderIdTextView.text = orderId
            toButton.setBackgroundResource(receivingCurrency.colorRes())
            toButton.text = receiving
            fromButton.setBackgroundResource(sendingCurrency.colorRes())
            fromButton.text = sending
            valueTextView.text = value
            feesTextView.text = fees
            receiveTextView.text = receiving
            sendToTextView.text = accountName
        }
    }

    companion object {
        private const val EXTRA_CONFIRMATION_MODEL =
            "com.blockchain.morph.ui.homebrew.exchange.locked.EXTRA_CONFIRMATION_MODEL"

        internal fun start(context: Context, model: ExchangeLockedModel) {
            Intent(context, ExchangeLockedActivity::class.java)
                .apply { putExtra(EXTRA_CONFIRMATION_MODEL, model) }
                .run { context.startActivity(this) }
        }
    }
}

@Parcelize
data class ExchangeLockedModel(
    val orderId: String,
    val value: String,
    val fees: String,
    val sending: String,
    val sendingCurrency: CryptoCurrency,
    val receiving: String,
    val receivingCurrency: CryptoCurrency,
    val accountName: String
) : Parcelable