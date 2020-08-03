package piuk.blockchain.android.ui.dashboard.assetdetails

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_dashboared_asset_details.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.dashboard.setDeltaColour
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.setOnTabSelectedListener
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AssetDetailSheet : SlidingModalBottomDialog() {

    val compositeDisposable = CompositeDisposable()

    private val currencyPrefs: CurrencyPrefs by inject()
    private val assetDetailsViewModel: AssetDetailsCalculator by scopedInject()
    private val locale = Locale.getDefault()

    interface Host : SlidingModalBottomDialog.Host {
        fun gotoSendFor(account: SingleAccount)
        fun goToReceiveFor(account: SingleAccount)
        fun gotoActivityFor(account: BlockchainAccount)
        fun gotoSwap(account: SingleAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a AssetDetailSheet.Host")
    }

    private val cryptoCurrency: CryptoCurrency by lazy {
        arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("No cryptoCurrency specified")
    }

    private val assetSelect: Coincore by scopedInject()
    private val token: CryptoAsset by lazy {
        assetSelect[cryptoCurrency]
    }

    override val layoutResource: Int
        get() = R.layout.dialog_dashboared_asset_details

    override fun initControls(view: View) {
        with(view) {

            configureChart(chart,
                getFiatSymbol(currencyPrefs.selectedFiatCurrency),
                cryptoCurrency.getDecimalPlaces())

            configureTabs(view.chart_price_periods)

            assetDetailsViewModel.token.accept(token)
            current_price_title.text =
                getString(R.string.dashboard_price_for_asset, cryptoCurrency.displayTicker)

            compositeDisposable += assetDetailsViewModel.assetDisplayDetails
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { },
                    onNext = { map ->
                        onGotAssetDetails(view, map)
                    }
                )

            compositeDisposable += assetDetailsViewModel.exchangeRate
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { },
                    onNext = { current_price.text = it }
                )

            compositeDisposable += assetDetailsViewModel.timeSpan.subscribeBy {
                configureUiForSelection(view, it)
            }

            compositeDisposable += assetDetailsViewModel
                .chartLoading
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { isLoading ->
                    if (isLoading) {
                        chartToLoadingState()
                    } else {
                        chartToDataState()
                    }
                }

            compositeDisposable += assetDetailsViewModel
                .historicPrices
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { data ->
                    chart.apply {
                        updateChart(chart, data)
                    }
                    updatePriceChange(view.price_change, data)
                }
        }
    }

    private fun onGotAssetDetails(view: View, assetDetails: AssetDisplayMap) {
        with(view) {

            asset_list.layoutManager = LinearLayoutManager(requireContext())
            asset_list.addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            val itemList = mutableListOf<AssetDetailItem>()

            assetDetails[AssetFilter.NonCustodial]?.let {
                itemList.add(
                    AssetDetailItem(
                        AssetFilter.NonCustodial,
                        it.account,
                        it.amount,
                        it.fiatValue,
                        it.actions,
                        it.interestRate
                    )
                )
            }

            assetDetails[AssetFilter.Custodial]?.let {
                itemList.add(
                    AssetDetailItem(
                        AssetFilter.Custodial,
                        it.account,
                        it.amount,
                        it.fiatValue,
                        it.actions,
                        it.interestRate
                    )
                )
            }

            assetDetails[AssetFilter.Interest]?.let {
                itemList.add(
                    AssetDetailItem(
                        AssetFilter.Interest,
                        it.account,
                        it.amount,
                        it.fiatValue,
                        it.actions,
                        it.interestRate
                    )
                )
            }

            asset_list.adapter = AssetDetailAdapter(itemList, ::onAssetActionSelected, analytics,
            cryptoCurrency.hasFeature(CryptoCurrency.CUSTODIAL_ONLY), token)
        }
    }

    private fun onAssetActionSelected(action: AssetAction, account: BlockchainAccount) {
        dismiss()
        when (action) {
            AssetAction.ViewActivity -> host.gotoActivityFor(selectAccount(account))
            AssetAction.Send -> host.gotoSendFor(selectAccount(account))
            AssetAction.Receive -> host.goToReceiveFor(selectAccount(account))
            AssetAction.Swap -> host.gotoSwap(selectAccount(account))
        }.exhaustive
    }

    private fun selectAccount(account: BlockchainAccount): SingleAccount =
        when (account) {
            is SingleAccount -> account
            is AccountGroup -> account.accounts
                    .firstOrNull { a -> a.isDefault }
                ?: account.accounts.firstOrNull()
                ?: throw IllegalStateException("No SingleAccount found")
            else -> throw IllegalStateException("Unknown account base")
        }

    private fun updateChart(chart: LineChart, data: List<PriceDatum>) {
        chart.apply {
            visible()
            clear()
            if (data.isEmpty()) {
                dialogView.price_change?.text = "--"
                return
            }
            val entries = data
                .filter { it.price != null }
                .map {
                    Entry(
                        it.timestamp.toFloat(),
                        it.price!!.toFloat())
                }

            this.data = LineData(LineDataSet(entries, null).apply {
                color = ContextCompat.getColor(context, getDataRepresentationColor(data))
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                setDrawCircles(false)
                isHighlightEnabled = true
                setDrawHighlightIndicators(false)
                marker = ValueMarker(
                    context,
                    R.layout.price_chart_marker,
                    getFiatSymbol(currencyPrefs.selectedFiatCurrency),
                    cryptoCurrency.getDecimalPlaces()
                )
            })
            animateX(500)
        }
    }

    private fun chartToLoadingState() {
        with(dialogView) {
            prices_loading?.visible()
            chart?.invisible()
            price_change?.apply {
                text = "--"
                setTextColor(ContextCompat.getColor(context, R.color.dashboard_chart_unknown))
            }
        }
    }

    private fun chartToDataState() {
        dialogView.prices_loading?.gone()
        dialogView.chart?.visible()
    }

    private fun configureTabs(chartPricePeriods: TabLayout) {
        TimeSpan.values().forEachIndexed { index, timeSpan ->
            chartPricePeriods.getTabAt(index)?.text = timeSpan.tabName()
        }
        chartPricePeriods.setOnTabSelectedListener {
            assetDetailsViewModel.timeSpan.accept(TimeSpan.values()[it])
        }
    }

    private fun TimeSpan.tabName() =
        when (this) {
            TimeSpan.ALL_TIME -> "ALL"
            TimeSpan.YEAR -> "1Y"
            TimeSpan.MONTH -> "1M"
            TimeSpan.WEEK -> "1W"
            TimeSpan.DAY -> "1D"
        }

    private fun getDataRepresentationColor(data: PriceSeries): Int {
        // We have filtered out nulls by here, so we can 'safely' default to zeros for the price
        val firstPrice: Double = data.first().price ?: 0.0
        val lastPrice: Double = data.last().price ?: 0.0

        val diff = lastPrice - firstPrice
        return if (diff < 0) R.color.dashboard_chart_negative else R.color.dashboard_chart_positive
    }

    @SuppressLint("SetTextI18n")
    private fun updatePriceChange(percentageView: AppCompatTextView, data: PriceSeries) {
        // We have filtered out nulls by here, so we can 'safely' default to zeros for the price
        val firstPrice: Double = data.firstOrNull()?.price ?: 0.0
        val lastPrice: Double = data.lastOrNull()?.price ?: 0.0
        val difference = lastPrice - firstPrice

        val percentChange = (difference / firstPrice) * 100
        val percentChangeTxt = if (percentChange.isNaN()) {
            "--"
        } else {
            String.format("%.1f", percentChange)
        }

        percentageView.text =
            FiatValue.fromMajor(
                currencyPrefs.selectedFiatCurrency,
                difference.toBigDecimal()
            ).toStringWithSymbol() + " ($percentChangeTxt%)"

        percentageView.setDeltaColour(
            delta = difference,
            negativeColor = R.color.dashboard_chart_negative,
            positiveColor = R.color.dashboard_chart_positive
        )
    }

    private fun configureChart(chart: LineChart, fiatSymbol: String, decimalPlaces: Int) {
        chart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)

            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return fiatSymbol + NumberFormat.getNumberInstance(Locale.getDefault())
                        .apply {
                            maximumFractionDigits = decimalPlaces
                            minimumFractionDigits = decimalPlaces
                            roundingMode = RoundingMode.HALF_UP
                        }.format(value)
                }
            }

            axisLeft.granularity = 0.005f
            axisLeft.isGranularityEnabled = true
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_grey_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_grey_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.isGranularityEnabled = true
            setExtraOffsets(8f, 0f, 0f, 10f)
            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_grey_medium))
            loadFont(
                context,
                CustomFont.MONTSERRAT_LIGHT
            ) {
                xAxis.typeface = it
                axisLeft.typeface = it
            }
        }
    }

    private fun configureUiForSelection(view: View, selection: TimeSpan) {
        val dateFormat = when (selection) {
            TimeSpan.ALL_TIME -> SimpleDateFormat("yyyy", locale)
            TimeSpan.YEAR -> SimpleDateFormat("MMM ''yy", locale)
            TimeSpan.MONTH, TimeSpan.WEEK -> SimpleDateFormat("dd. MMM", locale)
            TimeSpan.DAY -> SimpleDateFormat("H:00", locale)
        }

        val granularity = when (selection) {
            TimeSpan.ALL_TIME -> 60 * 60 * 24 * 365F
            TimeSpan.YEAR -> 60 * 60 * 24 * 30F
            TimeSpan.MONTH, TimeSpan.WEEK -> 60 * 60 * 24 * 2F
            TimeSpan.DAY -> 60 * 60 * 4F
        }

        with(view) {
            chart.chart.xAxis.apply {
                this.granularity = granularity
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong() * 1000))
                    }
                }
            }

            price_change_period.text = resources.getString(
                when (selection) {
                    TimeSpan.YEAR -> R.string.dashboard_time_span_last_year
                    TimeSpan.MONTH -> R.string.dashboard_time_span_last_month
                    TimeSpan.WEEK -> R.string.dashboard_time_span_last_week
                    TimeSpan.DAY -> R.string.dashboard_time_span_last_day
                    TimeSpan.ALL_TIME -> R.string.dashboard_time_span_all_time
                }
            )

            chart_price_periods.getTabAt(selection.ordinal)?.select()
        }
    }

    private fun CryptoCurrency.getDecimalPlaces(): Int =
        when (this) {
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.BCH,
            CryptoCurrency.PAX,
            CryptoCurrency.ALGO,
            CryptoCurrency.USDT -> 2
            CryptoCurrency.XLM -> 4
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto"

        fun newInstance(cryptoCurrency: CryptoCurrency): AssetDetailSheet {
            return AssetDetailSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                }
            }
        }

        private fun getFiatSymbol(currencyCode: String, locale: Locale = Locale.getDefault()) =
            Currency.getInstance(currencyCode).getSymbol(locale)
    }
}
