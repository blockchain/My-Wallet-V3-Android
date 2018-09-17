package com.blockchain.morph.ui.homebrew.exchange.detail

import android.os.Bundle
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import kotlinx.android.synthetic.main.activity_homebrew_trade_detail.*
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class HomebrewTradeDetailActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_detail)

        val trade = intent.extras.get("EXTRA_TRADE") as Trade
        setupToolbar(R.id.toolbar_constraint, trade.id)

        // TODO: data to test layout - use real object
        status.text = trade.state
        value.text = trade.depositQuantity
        receive.text = trade.quantity
        fees.text = trade.price
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }
}
