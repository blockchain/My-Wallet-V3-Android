package com.blockchain.morph.ui.homebrew.exchange

import android.os.Bundle
import android.widget.Button
import com.blockchain.morph.ui.R
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ExchangeHistoryActivity : BaseAuthActivity() {

    private lateinit var newExchangeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_history)

        newExchangeButton = findViewById(R.id.button_new_exchange)

        newExchangeButton.setOnClickListener {
            startActivity(ExchangeActivity.intent(this, "GBP"))
        }

        setupToolbar(R.id.toolbar_constraint, R.string.exchange)
    }
}
