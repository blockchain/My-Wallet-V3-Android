package piuk.blockchain.android.ui.shapeshift.overview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import kotlinx.android.synthetic.main.activity_shapeshift.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import com.blockchain.morph.ui.detail.TradeDetailActivity
import piuk.blockchain.android.ui.shapeshift.newexchange.NewExchangeActivity
import piuk.blockchain.android.ui.shapeshift.overview.adapter.TradesAdapter
import piuk.blockchain.android.ui.shapeshift.overview.adapter.TradesListClickListener
import piuk.blockchain.android.ui.shapeshift.stateselection.ShapeShiftStateSelectionActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

class ShapeShiftActivity : BaseMvpActivity<ShapeShiftView, ShapeShiftPresenter>(), ShapeShiftView,
    TradesListClickListener {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var shapeshiftPresenter: ShapeShiftPresenter

    private var tradesAdapter: TradesAdapter? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapeshift)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        shapeshift_retry_button.setOnClickListener { presenter.onRetryPressed() }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ShapeShiftStateSelectionActivity.STATE_SELECTION_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK
        ) {
            // State whitelisted - Reload
            onViewReady()
        } else {
            finish()
        }
    }

    private fun setUpRecyclerView(
        btcExchangeRate: Double,
        ethExchangeRate: Double,
        bchExchangeRate: Double,
        isBtc: Boolean
    ) {
        tradesAdapter = TradesAdapter(
            this,
            btcExchangeRate,
            ethExchangeRate,
            bchExchangeRate,
            isBtc,
            this
        )

        shapeshift_recycler_view.adapter = tradesAdapter
        shapeshift_recycler_view.layoutManager = LinearLayoutManager(this)

        tradesAdapter?.updateTradeList(emptyList())
    }

    override fun onExchangeRateUpdated(
        btcExchangeRate: Double,
        ethExchangeRate: Double,
        bchExchangeRate: Double,
        isBtc: Boolean
    ) {
        if (tradesAdapter == null) {
            setUpRecyclerView(btcExchangeRate, ethExchangeRate, bchExchangeRate, isBtc)
        } else {
            tradesAdapter?.onPriceUpdated(btcExchangeRate, ethExchangeRate)
        }
    }

    override fun onStateUpdated(shapeshiftState: ShapeShiftState) = when (shapeshiftState) {
        is ShapeShiftState.Data -> onData(shapeshiftState)
        ShapeShiftState.Empty -> onEmptyLayout()
        ShapeShiftState.Error -> onError()
        ShapeShiftState.Loading -> onLoading()
    }

    override fun onTradeUpdate(trade: Trade, tradeResponse: TradeStatusResponse) {
        tradesAdapter?.updateTrade(trade, tradeResponse)
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    override fun createPresenter() = shapeshiftPresenter

    override fun getView() = this

    private fun onEmptyLayout() {
        onLoading()
        NewExchangeActivity.start(this)
        // Remove self from back stack
        finish()
    }

    private fun onError() {
        shapeshift_loading_layout.gone()
        shapeshift_error_layout.visible()
        shapeshift_recycler_view.gone()
    }

    private fun onLoading() {
        shapeshift_loading_layout.visible()
        shapeshift_error_layout.gone()
        shapeshift_recycler_view.gone()
    }

    private fun onData(data: ShapeShiftState.Data) {

        tradesAdapter?.updateTradeList(data.trades)

        shapeshift_loading_layout.gone()
        shapeshift_error_layout.gone()
        shapeshift_recycler_view.visible()
    }

    override fun onViewTypeChanged(isBtc: Boolean) {
        tradesAdapter?.onViewFormatUpdated(isBtc)
    }

    override fun onTradeClicked(depositAddress: String) {
        TradeDetailActivity.start(this, depositAddress)
    }

    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    override fun onNewExchangeClicked() {
        NewExchangeActivity.start(this)
    }

    override fun showStateSelection() {
        ShapeShiftStateSelectionActivity.start(
            this,
            ShapeShiftStateSelectionActivity.STATE_SELECTION_REQUEST_CODE
        )
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, ShapeShiftActivity::class.java))
        }
    }
}