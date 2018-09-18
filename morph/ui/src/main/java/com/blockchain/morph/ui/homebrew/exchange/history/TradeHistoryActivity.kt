package com.blockchain.morph.ui.homebrew.exchange.history

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.R.layout.activity_homebrew_trade_history
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewNavHostActivity
import com.blockchain.morph.ui.homebrew.exchange.detail.HomebrewTradeDetailActivity
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import kotlinx.android.synthetic.main.activity_homebrew_trade_history.*
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class TradeHistoryActivity : BaseAuthActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_homebrew_trade_history)

        button_new_exchange.setOnClickListener {
            HomebrewNavHostActivity.start(this)
        }

        setupToolbar(R.id.toolbar_constraint, R.string.exchange)

        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager

        val trades = createTestData()
        recyclerView.adapter = TradeHistoryAdapter(trades) { trade: Trade -> tradeClicked(trade) }
    }

    private fun tradeClicked(trade: Trade) {
        val showDetailActivityIntent = Intent(this, HomebrewTradeDetailActivity::class.java)
        showDetailActivityIntent.putExtra("EXTRA_TRADE", trade)
        startActivity(showDetailActivityIntent)
    }

    // TODO: Fake data to test the layout
    private fun createTestData(): List<Trade> {
        val trades: ArrayList<Trade> = ArrayList()
        trades.add(
            0,
            Trade(
                "ede39566-1f0d-4e48-96fa-b558b70e46b7",
                "FINISHED",
                "ETH",
                "0.06",
                "BTC-ETH",
                "0.1345",
                "01.02.2018",
                "0.008022"
            )
        )
        trades.add(
            1,
            Trade(
                "adf34565-1f0d-4e48-96fa-b558b70e4ss4",
                "FINISHED",
                "BTC",
                "0.04",
                "ETH-BTC",
                "2.6578",
                "01.02.2018",
                "0.008022"
            )
        )
        trades.add(
            2,
            Trade(
                "ght32544-1f0d-4e48-96fa-b558b70e45tt",
                "PENDING_WITHDRAWAL",
                "BTH",
                "0.24",
                "BTC-BTH",
                "4.3333",
                "01.02.2018",
                "0.008022"
            )
        )
        trades.add(
            3,
            Trade(
                "ddd31555-1f0d-4e48-96fa-b558b70e4654",
                "PENDING_DEPOSIT",
                "ETH",
                "4.10",
                "BTC-ETH",
                "0.2222",
                "01.02.2018",
                "0.008022"
            )
        )
        return trades
    }
}
