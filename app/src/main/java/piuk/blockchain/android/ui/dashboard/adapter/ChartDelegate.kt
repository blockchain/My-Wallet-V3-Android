package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.item_chart.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.ChartDisplayable
import piuk.blockchain.android.ui.dashboard.ChartsState
import piuk.blockchain.android.util.extensions.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import uk.co.chrisjenx.calligraphy.CalligraphyUtils
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import java.text.SimpleDateFormat
import java.util.*

class ChartDelegate<in T>(
        private val activity: Activity
) : AdapterDelegate<T> {

    private var viewHolder: ChartViewHolder? = null

    private val typefaceRegular by unsafeLazy {
        TypefaceUtils.load(
                activity.assets,
                "fonts/Montserrat-Regular.ttf"
        )
    }
    private val typefaceLight by unsafeLazy {
        TypefaceUtils.load(
                activity.assets,
                "fonts/Montserrat-Light.ttf"
        )
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is ChartDisplayable

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            ChartViewHolder(parent.inflate(R.layout.item_chart))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {
        viewHolder = holder as ChartViewHolder
        setTextViewSelected(holder.year, listOf(holder.week, holder.month))
    }

    internal fun updateChartState(chartsState: ChartsState) {
        return when (chartsState) {
            is ChartsState.Data -> showData(chartsState)
            is ChartsState.Loading -> showLoading()
            is ChartsState.Error -> showError()
            is ChartsState.TimeSpanUpdated -> showTimeSpanSelected(chartsState.timeSpan)
        }
    }

    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        viewHolder?.currency?.setText(
                if (cryptoCurrency == CryptoCurrencies.BTC)
                    R.string.dashboard_bitcoin_price
                else
                    R.string.dashboard_ether_price
        )
    }

    internal fun updateCurrencyPrice(price: String) {
        viewHolder?.let { it.price.text = price }
    }

    private fun showData(data: ChartsState.Data) {
        configureChart(data.fiatSymbol)

        viewHolder?.let {
            it.week.setOnClickListener { data.getChartWeek() }
            it.month.setOnClickListener { data.getChartMonth() }
            it.year.setOnClickListener { data.getChartYear() }

            it.progressBar.gone()
            it.chart.apply {
                visible()

                val entries = data.data.map { Entry(it.x, it.y) }
                this.data = LineData(LineDataSet(entries, null).apply {
                    color = ContextCompat.getColor(activity, R.color.primary_navy_medium)
                    lineWidth = 3f
                    mode = LineDataSet.Mode.LINEAR
                    setDrawValues(false)
                    circleRadius = 1.5f
                    setDrawCircleHole(false)
                    setCircleColor(ContextCompat.getColor(activity, R.color.primary_navy_medium))
                    setDrawFilled(false)
                })

                animateX(500)
            }
        }
    }

    private fun showLoading() {
        viewHolder?.let {
            it.progressBar.visible()
            it.chart.invisible()
        }
    }

    private fun showError() {
        viewHolder?.let {
            it.progressBar.gone()
            it.chart.apply {
                visible()
                data = null
                invalidate()
            }
        }

        activity.toast(R.string.dashboard_charts_error, ToastCustom.TYPE_ERROR)
    }

    private fun showTimeSpanSelected(timeSpan: TimeSpan) {
        selectButton(timeSpan, viewHolder)
    }

    private fun selectButton(timeSpan: TimeSpan, viewHolder: ChartViewHolder?) {
        viewHolder?.let {
            when (timeSpan) {
                TimeSpan.WEEK -> setTextViewSelected(viewHolder.week, listOf(viewHolder.month, viewHolder.year))
                TimeSpan.MONTH -> setTextViewSelected(viewHolder.month, listOf(viewHolder.week, viewHolder.year))
                TimeSpan.YEAR -> setTextViewSelected(viewHolder.year, listOf(viewHolder.week, viewHolder.month))
                else -> throw IllegalArgumentException("Day isn't currently supported")
            }
        }
    }

    private fun setTextViewSelected(selected: TextView, unselected: List<TextView>) {
        with(selected) {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            CalligraphyUtils.applyFontToTextView(this, typefaceRegular)
        }
        unselected.map {
            with(it) {
                paintFlags = paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                CalligraphyUtils.applyFontToTextView(this, typefaceLight)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun configureChart(fiatSymbol: String) {
        viewHolder?.chart?.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            isHighlightPerTapEnabled = false
            isHighlightPerDragEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisLeft.setValueFormatter { fl, _ -> "$fiatSymbol${fl.toInt()}" }
            axisLeft.typeface = typefaceLight
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.typeface = typefaceLight
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setValueFormatter { fl, _ ->
                SimpleDateFormat("MMM dd").format(Date(fl.toLong() * 1000))
            }
        }
    }


    private class ChartViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var chart: LineChart = itemView.chart
        internal var week: TextView = itemView.textview_week
        internal var month: TextView = itemView.textview_month
        internal var year: TextView = itemView.textview_year
        internal var price: TextView = itemView.textview_price
        internal var currency: TextView = itemView.textview_currency
        internal var progressBar: ProgressBar = itemView.progress_bar

    }

}