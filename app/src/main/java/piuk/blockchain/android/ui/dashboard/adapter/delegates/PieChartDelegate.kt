package piuk.blockchain.android.ui.dashboard.adapter.delegates

import android.content.Context
import android.graphics.Color
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.lockbox.ui.LockboxLandingActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.item_pie_chart_bitcoin_unspendable.view.*
import kotlinx.android.synthetic.main.item_pie_chart_card.view.*
import kotlinx.android.synthetic.main.item_pie_chart_lockbox.view.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.BalanceFilter
import piuk.blockchain.android.ui.dashboard.DashboardConfig
import piuk.blockchain.android.ui.dashboard.PieChartsState
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont

class PieChartDelegate<in T>(
    private val context: Context,
    private val coinSelector: (CryptoCurrency) -> Unit,
    private val balanceModeSelector: (BalanceFilter) -> Unit
) : AdapterDelegate<T> {

    private var viewHolder: PieChartViewHolder? = null

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is PieChartsState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        PieChartViewHolder(
            parent.inflate(R.layout.item_pie_chart_lockbox),
            coinSelector,
            balanceModeSelector
        )

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
        val isEmpty = data.isZero
        configureChart(isEmpty)
        displayLockboxDisclaimer(data.hasLockbox)
        displayBalanceSpinner(data.hasLockbox && BuildConfig.SHOW_LOCKBOX_BALANCE)

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

            DashboardConfig.currencies.forEach {
                valueTextView(it).text = data[it].displayable.fiatValueString
                amountTextView(it).text = data[it].displayable.cryptoValueString
            }

            nonSpendableDataPoint = data.bitcoin.watchOnly

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
        DashboardConfig.currencies.map {
            data[it].displayable withLabel context.getString(it.label())
        }
    }

    private infix fun PieChartsState.DataPoint.withLabel(
        label: String
    ) = PieEntry(fiatValue.toBigDecimal().toFloat(), label, this)

    private fun getCoinColors(empty: Boolean): List<Int> = if (empty) {
        listOf(ContextCompat.getColor(context, R.color.primary_gray_light))
    } else {
        DashboardConfig.currencies.map {
            ContextCompat.getColor(context, it.colorRes())
        }
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

            if (empty) animateY(1000, Easing.EasingOption.EaseInOutQuad)
            isRotationEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setDrawEntryLabels(false)

            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
            if (!empty) marker = ValueMarker(context, R.layout.item_pie_chart_marker)
        }
    }

    private fun displayLockboxDisclaimer(show: Boolean) {
        viewHolder?.lockBoxDisclaimer?.apply {
            val lockboxName = context.getString(R.string.lockbox_title)
            val lockboxString = context.getString(R.string.dashboard_lockbox_disclaimer, lockboxName)
            SpannableString(lockboxString).apply {
                setSpan(
                    ForegroundColorSpan(context.getResolvedColor(R.color.primary_blue_accent)),
                    lockboxString.length - (lockboxName.length + 1),
                    lockboxString.length - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }.run {
                this@apply.text = this
                this@apply.goneIf(!show)
            }
        }
    }

    private fun displayBalanceSpinner(show: Boolean) {
        viewHolder?.balanceSpinnerCard.goneIf(!show)
    }

    private inner class ValueMarker(
        context: Context,
        layoutResource: Int
    ) : MarkerView(context, layoutResource) {

        private val coin = findViewById<TextView>(R.id.textview_marker_coin)
        private val price = findViewById<TextView>(R.id.textview_marker_price)

        private var mpPointF: MPPointF? = null

        override fun refreshContent(e: Entry, highlight: Highlight) {
            val pieEntry = e as PieEntry
            val dataPoint = e.data as PieChartsState.DataPoint
            coin.text = pieEntry.label
            price.text = dataPoint.fiatValueString
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
        private val coinSelector: (CryptoCurrency) -> Unit,
        private val balanceModeSelector: (BalanceFilter) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private var displayNonSpendableAsFiat = false
            set(value) {
                if (value == field) return
                field = value
                updateNonSpendable(nonSpendableDataPoint)
            }

        var nonSpendableDataPoint: PieChartsState.DataPoint? = null
            set(value) {
                if (value == field) return
                field = value
                updateNonSpendable(field)
            }

        private fun updateNonSpendable(nonSpendableDataPoint: PieChartsState.DataPoint?) {
            bitcoinNonSpendablePane.goneIf(nonSpendableDataPoint?.isZero ?: true)
            nonSpendableDataPoint?.let {
                bitcoinNonSpendableValue.text =
                    itemView.context.getString(
                        R.string.dashboard_non_spendable_value,
                        if (displayNonSpendableAsFiat) it.fiatValueString else it.cryptoValueString
                    )
            }
        }

        internal var chart: PieChart = itemView.pie_chart
        internal val progressBar: ProgressBar = itemView.progress_bar

        // Bitcoin
        internal var bitcoinButton: LinearLayout = itemView.linear_layout_bitcoin
        internal var bitcoinNonSpendableValue: TextView = itemView.textview_bitcoin_non_spendable_toggle.apply {
            setOnClickListener {
                displayNonSpendableAsFiat = !displayNonSpendableAsFiat
            }
        }
        internal var bitcoinNonSpendablePane: View = itemView.non_spendable_pane
        // Ether
        internal var etherButton: LinearLayout = itemView.linear_layout_ether
        // Bitcoin Cash
        internal var bitcoinCashButton: LinearLayout = itemView.linear_layout_bitcoin_cash
        // lumens
        internal var lumensButton: LinearLayout = itemView.linear_layout_lumens

        internal fun valueTextView(cryptoCurrency: CryptoCurrency) =
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> itemView.textview_value_bitcoin
                CryptoCurrency.ETHER -> itemView.textview_value_ether
                CryptoCurrency.BCH -> itemView.textview_value_bitcoin_cash
                CryptoCurrency.XLM -> itemView.textview_value_lumens
            }

        internal fun amountTextView(cryptoCurrency: CryptoCurrency) =
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> itemView.textview_amount_bitcoin
                CryptoCurrency.ETHER -> itemView.textview_amount_ether
                CryptoCurrency.BCH -> itemView.textview_amount_bitcoin_cash
                CryptoCurrency.XLM -> itemView.textview_amount_lumens
            }

        // Lockbox
        internal var lockBoxDisclaimer: TextView = itemView.text_view_lockbox_disclaimer

        internal var balanceSpinner: Spinner = itemView.spinner_balance_selection

        internal var balanceSpinnerCard: View = itemView.card_balance_selection

        init {
            bitcoinButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.BTC) }
            etherButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.ETHER) }
            bitcoinCashButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.BCH) }
            lumensButton.setOnClickListener { coinSelector.invoke(CryptoCurrency.XLM) }
            lockBoxDisclaimer.setOnClickListener { LockboxLandingActivity.start(context) }
            balanceSpinner.addOptions()
        }

        private fun Spinner.addOptions() {
            adapter = ArrayAdapter.createFromResource(
                context,
                R.array.balance_type,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position) {
                        0 -> balanceModeSelector(BalanceFilter.Total)
                        1 -> balanceModeSelector(BalanceFilter.Wallet)
                        2 -> balanceModeSelector(BalanceFilter.ColdStorage)
                    }
                }
            }
        }
    }
}

@StringRes
private fun CryptoCurrency.label() =
    when (this) {
        CryptoCurrency.BTC -> R.string.bitcoin
        CryptoCurrency.ETHER -> R.string.ether
        CryptoCurrency.BCH -> R.string.bitcoin_cash
        CryptoCurrency.XLM -> R.string.lumens
    }
