package piuk.blockchain.android.ui.charts

import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.charts.models.ChartDatumDto
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class ChartsPresenter @Inject constructor(
    private val chartsDataManager: ChartsDataManager,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val prefsUtil: PrefsUtil,
    private val currencyFormatManager: CurrencyFormatManager
) : BasePresenter<ChartsView>() {

    internal var selectedTimeSpan by Delegates.observable(TimeSpan.MONTH) { _, _, new ->
        updateChartsData(new)
    }

    override fun onViewReady() {
        selectedTimeSpan = TimeSpan.MONTH
    }

    private fun updateChartsData(timeSpan: TimeSpan) {
        compositeDisposable.clear()
        getCurrentPrice()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))

        when (timeSpan) {
            TimeSpan.ALL_TIME -> chartsDataManager.getAllTimePrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.YEAR -> chartsDataManager.getYearPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.DAY -> chartsDataManager.getDayPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
        }.addToCompositeDisposable(this)
            .toList()
            .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
            .doOnSubscribe { view.updateSelectedCurrency(view.cryptoCurrency) }
            .doOnSuccess { view.updateChartState(getChartsData(it)) }
            .doOnError { view.updateChartState(ChartsState.Error) }
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    private fun getChartsData(list: List<ChartDatumDto>) =
        ChartsState.Data(list, getCurrencySymbol())

    private fun getCurrentPrice() {
        val price = exchangeRateFactory.getLastPrice(view.cryptoCurrency, getFiatCurrency())
        view.updateCurrentPrice(getCurrencySymbol(), price)
    }

    private fun getFiatCurrency() =
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getCurrencySymbol() =
        currencyFormatManager.getFiatSymbol(getFiatCurrency(), view.locale)
}

sealed class ChartsState {

    data class Data(
        val data: List<ChartDatumDto>,
        val fiatSymbol: String
    ) : ChartsState()

    class TimeSpanUpdated(val timeSpan: TimeSpan) : ChartsState()
    object Loading : ChartsState()
    object Error : ChartsState()
}