package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.item_pie_chart.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.PieChartsState
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class PieChartDelegate<in T>(
    private val context: Context,
    private val coinSelector: (CryptoCurrency) -> Unit
) : AdapterDelegate<T> {

    private var viewHolder: PieChartViewHolder? = null
    private var fiatSymbol: String? = null
    // For working out labels in ValueMarker class
    private var bitcoinValue = BigDecimal.ZERO
    private var etherValue = BigDecimal.ZERO
    private var bitcoinCashValue = BigDecimal.ZERO
    private var firstRender = true

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is PieChartsState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        PieChartViewHolder(parent.inflate(R.layout.item_pie_chart), coinSelector)

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        viewHolder = holder as PieChartViewHolder
    }

    internal fun updateChartState(pieChartsState: PieChartsState) = when (pieChartsState) {
        is PieChartsState.Data -> renderData(pieChartsState)
        PieChartsState.Error -> renderError()
        PieChartsState.Loading -> renderLoading()
    }

    private fun renderLoading() {
        viewHolder?.apply {
            progressBar.visible()
            chart.invisible()
        }
    }

    private fun renderError() {
        viewHolder?.apply {
            progressBar.gone()
            chart.apply {
                visible()
                data = null
                invalidate()
            }
        }

        context.toast(R.string.dashboard_charts_balance_error, ToastCustom.TYPE_ERROR)
    }

    private fun renderData(data: PieChartsState.Data) {
        // Store values for comparisons
        bitcoinValue = data.bitcoinValue
        etherValue = data.etherValue
        bitcoinCashValue = data.bitcoinCashValue
        fiatSymbol = data.fiatSymbol

        val isEmpty = (data.bitcoinValue + data.etherValue + data.bitcoinCashValue)
            .compareTo(BigDecimal.ZERO) == 0
        // Prevent issue where chart won't render if NOT first fun AND data has recently gone from
        // empty to non-empty.
        if (isEmpty) firstRender = true
        configureChart(isEmpty)

        val entries = getEntries(isEmpty, data)
        val coinColors = getCoinColors(isEmpty)

        val dataSet = PieDataSet(entries, context.getString(R.string.dashboard_balances)).apply {
            setDrawIcons(false)
            sliceSpace = 0f
            selectionShift = 5f
            colors = coinColors
        }

        val chartData = PieData(dataSet).apply { setDrawValues(false) }

        viewHolder?.apply {
            bitcoinValue.text = data.bitcoinValueString
            etherValue.text = data.etherValueString
            bitcoinCashValue.text = data.bitcoinCashValueString
            bitcoinAmount.text = data.bitcoinAmountString
            etherAmount.text = data.etherAmountString
            bitcoinCashAmount.text = data.bitcoinCashAmountString

            progressBar.gone()
            chart.apply {
                centerText = data.totalValueString
                this.data = chartData
                highlightValues(null)
                invalidate()
                visible()
            }
        }
    }

    private fun getEntries(empty: Boolean, data: PieChartsState.Data): List<PieEntry> = if (empty) {
        listOf(PieEntry(100.0f, ""))
    } else {
        listOf(
            PieEntry(data.bitcoinValue.toFloat(), context.getString(R.string.bitcoin)),
            PieEntry(data.etherValue.toFloat(), context.getString(R.string.ether)),
            PieEntry(data.bitcoinCashValue.toFloat(), context.getString(R.string.bitcoin_cash))
        )
    }

    private fun getCoinColors(empty: Boolean): List<Int> = if (empty) {
        listOf(ContextCompat.getColor(context, R.color.primary_gray_light))
    } else {
        listOf(
            ContextCompat.getColor(context, R.color.color_bitcoin),
            ContextCompat.getColor(context, R.color.color_ether),
            ContextCompat.getColor(context, R.color.color_bitcoin_cash)
        )
    }

    private fun configureChart(empty: Boolean) {
        viewHolder?.chart?.apply {
            setDrawCenterText(true)
            loadFont(
                context,
                CustomFont.MONTSERRAT_REGULAR
            ) { setCenterTextTypeface(it) }
            setCenterTextColor(ContextCompat.getColor(context, R.color.primary_gray_dark))
            setCenterTextSize(16f)

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 70f

            if (firstRender) animateY(1000, Easing.EasingOption.EaseInOutQuad)
            isRotationEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setDrawEntryLabels(false)

            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
            if (!empty) marker = ValueMarker(context, R.layout.item_pie_chart_marker)
        }
        firstRender = false
    }

    private inner class ValueMarker(
        context: Context,
        layoutResource: Int
    ) : MarkerView(context, layoutResource) {

        private val coin = findViewById<TextView>(R.id.textview_marker_coin)
        private val price = findViewById<TextView>(R.id.textview_marker_price)

        private var mpPointF: MPPointF? = null

        @SuppressLint("SetTextI18n")
        override fun refreshContent(e: Entry, highlight: Highlight) {
            val amount = e.y.toBigDecimal()
            price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                .apply {
                    maximumFractionDigits = 2
                    minimumFractionDigits = 2
                }
                .format(amount)}"

            // Rounded for comparison as at some point in the stack we're truncating full BigDecimals
            // to doubles and then converting them back again
            coin.text = when (amount.setScale(2, RoundingMode.HALF_UP)) {
                bitcoinValue.setScale(
                    2,
                    RoundingMode.HALF_UP
                ) -> context.getString(R.string.bitcoin)
                etherValue.setScale(2, RoundingMode.HALF_UP) -> context.getString(R.string.ether)
                bitcoinCashValue.setScale(
                    2,
                    RoundingMode.HALF_UP
                ) -> context.getString(R.string.bitcoin_cash)
                else -> ""
            }

            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            if (mpPointF == null) {
                // Center the marker horizontally and vertically
                mpPointF = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
            }

            return mpPointF!!
        }
    }

    private class PieChartViewHolder internal constructor(
        itemView: View,
        private val coinSelector: (CryptoCurrency) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        internal var chart: PieChart = itemView.pie_chart
        internal val progressBar: ProgressBar = itemView.progress_bar
        // Bitcoin
        internal var bitcoinValue: TextView = itemView.textview_value_bitcoin
        internal var bitcoinAmount: TextView = itemView.textview_amount_bitcoin
        internal var bitcoinButton: LinearLayout = itemView.linear_layout_bitcoin
        // Ether
        internal var etherValue: TextView = itemView.textview_value_ether
        internal var etherAmount: TextView = itemView.textview_amount_ether
        internal var etherButton: LinearLayout = itemView.linear_layout_ether
        // Bitcoin Cash
        internal var bitcoinCashValue: TextView = itemView.textview_value_bitcoin_cash
        internal var bitcoinCashAmount: TextView = itemView.textview_amount_bitcoin_cash
        internal var bitcoinCashButton: LinearLayout = itemView.linear_layout_bitcoin_cash

        init {
            bitcoinButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.BTC) }
            etherButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.ETHER) }
            bitcoinCashButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.BCH) }
        }
    }
}