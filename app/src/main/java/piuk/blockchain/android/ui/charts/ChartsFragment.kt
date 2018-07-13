package piuk.blockchain.android.ui.charts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.fragment_graphs.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcore.data.charts.TimeSpan
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.setCustomFont
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ChartsFragment : BaseFragment<ChartsView, ChartsPresenter>(), ChartsView {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var chartsPresenter: ChartsPresenter

    override val locale: Locale = Locale.getDefault()
    override val cryptoCurrency: CryptoCurrency by unsafeLazy {
        arguments!!.getSerializable(ARGUMENT_CRYPTOCURRENCY) as CryptoCurrency
    }
    private val buttonsList by unsafeLazy {
        listOf(
            textview_day,
            textview_week,
            textview_month,
            textview_year,
            textview_all_time
        )
    }
    private var listener: TimeSpanUpdateListener? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_graphs)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady()
    }

    override fun updateChartState(state: ChartsState) = when (state) {
        is ChartsState.Data -> showData(state)
        ChartsState.Loading -> showLoading()
        ChartsState.Error -> showError()
        is ChartsState.TimeSpanUpdated -> showTimeSpanSelected(state.timeSpan)
    }

    override fun updateSelectedCurrency(cryptoCurrency: CryptoCurrency) {
        textview_currency.text = getString(R.string.dashboard_price, cryptoCurrency.unit)
    }

    @SuppressLint("SetTextI18n")
    override fun updateCurrentPrice(fiatSymbol: String, price: Double) {
        textview_price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
            .apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
                roundingMode = RoundingMode.HALF_UP
            }.format(price)}"
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is TimeSpanUpdateListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement TimeSpanUpdateListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun createPresenter() = chartsPresenter

    override fun getMvpView() = this

    /**
     * Updates the Presenter's [TimeSpan] variable, which then notifies the rest of the Presenter
     * to update the UI.
     */
    internal fun onTimeSpanUpdated(timeSpan: TimeSpan) {
        presenter.selectedTimeSpan = timeSpan
    }

    private fun showTimeSpanSelected(timeSpan: TimeSpan) {
        selectButton(timeSpan)
        setDateFormatter(timeSpan)
    }

    private fun selectButton(timeSpan: TimeSpan) {
        when (timeSpan) {
            TimeSpan.ALL_TIME -> setTextViewSelected(textview_all_time)
            TimeSpan.YEAR -> setTextViewSelected(textview_year)
            TimeSpan.MONTH -> setTextViewSelected(textview_month)
            TimeSpan.WEEK -> setTextViewSelected(textview_week)
            TimeSpan.DAY -> setTextViewSelected(textview_day)
        }
    }

    private fun setTextViewSelected(selected: TextView) {
        with(selected) {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setCustomFont(CustomFont.MONTSERRAT_REGULAR)
        }
        buttonsList.filterNot { it === selected }
            .map {
                with(it) {
                    paintFlags = paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                    setCustomFont(CustomFont.MONTSERRAT_LIGHT)
                }
            }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setDateFormatter(timeSpan: TimeSpan) {
        val dateFormat = when (timeSpan) {
            TimeSpan.ALL_TIME -> SimpleDateFormat("yyyy")
            TimeSpan.YEAR -> SimpleDateFormat("MMM ''yy")
            TimeSpan.MONTH, TimeSpan.WEEK -> SimpleDateFormat("dd. MMM")
            TimeSpan.DAY -> SimpleDateFormat("H:00")
        }

        val granularity = when (timeSpan) {
            TimeSpan.ALL_TIME -> 60 * 60 * 24 * 365F
            TimeSpan.YEAR -> 60 * 60 * 24 * 30F
            TimeSpan.MONTH, TimeSpan.WEEK -> 60 * 60 * 24 * 2F
            TimeSpan.DAY -> 60 * 60 * 4F
        }

        chart.xAxis.apply {
            // Minimum between values
            this.granularity = granularity
            setValueFormatter { fl, _ ->
                dateFormat.format(Date(fl.toLong() * 1000))
            }
        }
    }

    private fun showData(data: ChartsState.Data) {
        configureChart()
        updatePercentChange(data)

        textview_day.setOnClickListener { listener?.onTimeSpanUpdated(TimeSpan.DAY) }
        textview_week.setOnClickListener { listener?.onTimeSpanUpdated(TimeSpan.WEEK) }
        textview_month.setOnClickListener { listener?.onTimeSpanUpdated(TimeSpan.MONTH) }
        textview_year.setOnClickListener { listener?.onTimeSpanUpdated(TimeSpan.YEAR) }
        textview_all_time.setOnClickListener { listener?.onTimeSpanUpdated(TimeSpan.ALL_TIME) }

        progress_bar.invisible()
        chart.apply {
            visible()

            val entries = data.data.map { Entry(it.timestamp.toFloat(), it.price.toFloat()) }
            this.data = LineData(LineDataSet(entries, null).apply {
                color = ContextCompat.getColor(context, R.color.primary_navy_medium)
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                setDrawCircles(false)
                isHighlightEnabled = true
                setDrawHighlightIndicators(false)
                marker = ValueMarker(context, R.layout.item_chart_marker, data.fiatSymbol)
            })

            animateX(500)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePercentChange(data: ChartsState.Data) {
        val first = data.data.first()
        val last = data.data.last()
        val difference = last.price - first.price
        val percentChange = (difference / first.price) * 100

        textview_percentage.text = "${String.format("%.1f", percentChange)}%"
        when {
            percentChange < 0 -> updateArrow(imageview_arrow, 0f, R.color.product_red_medium)
            percentChange == 0.0 -> imageview_arrow.invisible()
            else -> updateArrow(imageview_arrow, 180f, R.color.product_green_medium)
        }
    }

    private fun updateArrow(arrow: ImageView, rotation: Float, @ColorRes color: Int) {
        arrow.visible()
        arrow.rotation = rotation
        arrow.setColorFilter(
            ContextCompat.getColor(arrow.context, color),
            PorterDuff.Mode.SRC_ATOP
        )
    }

    private fun showLoading() {
        progress_bar.visible()
        chart.invisible()
    }

    private fun showError() {
        progress_bar.invisible()
        chart.apply {
            visible()
            data = null
            invalidate()
        }

        toast(R.string.dashboard_charts_error, ToastCustom.TYPE_ERROR)
    }

    @SuppressLint("SimpleDateFormat")
    private fun configureChart() {
        chart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisLeft.setValueFormatter { fl, _ ->
                NumberFormat.getNumberInstance(Locale.getDefault())
                    .apply {
                        maximumFractionDigits = 0
                        roundingMode = RoundingMode.HALF_UP
                    }.format(fl)
            }
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.isGranularityEnabled = true
            setExtraOffsets(8f, 0f, 0f, 10f)
            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
            loadFont(
                context,
                CustomFont.MONTSERRAT_LIGHT
            ) {
                xAxis.typeface = it
                axisLeft.typeface = it
            }
        }
    }

    companion object {

        private const val ARGUMENT_CRYPTOCURRENCY = "ARGUMENT_CRYPTOCURRENCY"

        internal fun newInstance(cryptoCurrency: CryptoCurrency): ChartsFragment {
            val args = Bundle().apply {
                putSerializable(ARGUMENT_CRYPTOCURRENCY, cryptoCurrency)
            }
            return ChartsFragment().apply { arguments = args }
        }
    }

    inner class ValueMarker(
        context: Context,
        layoutResource: Int,
        private val fiatSymbol: String
    ) : MarkerView(context, layoutResource) {

        private val date = findViewById<TextView>(R.id.textview_marker_date)
        private val price = findViewById<TextView>(R.id.textview_marker_price)

        private var mpPointF: MPPointF? = null

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun refreshContent(e: Entry, highlight: Highlight) {
            date.text = SimpleDateFormat("E, MMM dd, HH:mm").format(Date(e.x.toLong() * 1000))
            price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                .apply {
                    maximumFractionDigits = 2
                    minimumFractionDigits = 2
                }
                .format(e.y)}"

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
}

interface TimeSpanUpdateListener {

    fun onTimeSpanUpdated(timeSpan: TimeSpan)
}