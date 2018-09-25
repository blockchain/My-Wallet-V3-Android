package com.blockchain.morph.ui.homebrew.exchange.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Button
import android.widget.TextView
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.detail.HomebrewTradeDetailActivity
import com.blockchain.morph.ui.homebrew.exchange.history.adapter.TradeHistoryAdapter
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewNavHostActivity
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.util.Locale

class TradeHistoryActivity : BaseMvpActivity<TradeHistoryView, TradeHistoryPresenter>(),
    TradeHistoryView {

    override val locale: Locale = Locale.getDefault()
    private val presenter: TradeHistoryPresenter by inject()
    private val fiat: CurrencyState by inject()
    private val tradeHistoryAdapter = TradeHistoryAdapter(this::tradeClicked)
    private val buttonNewExchange by unsafeLazy { findViewById<Button>(R.id.button_new_exchange) }
    private val swipeLayout by unsafeLazy { findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_homebrew_history) }
    private val emptyState by unsafeLazy { findViewById<TextView>(R.id.emptyState) }
    private val recyclerView by unsafeLazy { findViewById<RecyclerView>(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_history)

        buttonNewExchange.setOnClickListener {
            HomebrewNavHostActivity.start(this, fiat.fiatUnit)
        }

        setupToolbar(R.id.toolbar_constraint, R.string.exchange)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TradeHistoryActivity)
            adapter = tradeHistoryAdapter
        }

        swipeLayout.setOnRefreshListener { onViewReady() }

        onViewReady()
    }

    override fun renderUi(uiState: ExchangeUiState) {
        when (uiState) {
            is ExchangeUiState.Data -> renderData(uiState)
            ExchangeUiState.Error -> renderError()
            ExchangeUiState.Empty -> renderError()
            ExchangeUiState.Loading -> swipeLayout.isRefreshing = true
        }
    }

    private fun renderData(uiState: ExchangeUiState.Data) {
        tradeHistoryAdapter.items = uiState.trades
        recyclerView.visible()
        swipeLayout.isRefreshing = false
    }

    private fun renderError() {
        swipeLayout.isRefreshing = false
        emptyState.visible()
        recyclerView.gone()
    }

    private fun tradeClicked(trade: Trade) {
        Intent(this, HomebrewTradeDetailActivity::class.java).apply {
            putExtra("EXTRA_TRADE", trade)
        }.run { startActivity(this) }
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): TradeHistoryPresenter = presenter

    override fun getView(): TradeHistoryView = this

    companion object {

        fun start(context: Context) {
            Intent(context, TradeHistoryActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}
