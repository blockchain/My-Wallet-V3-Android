package piuk.blockchain.android.data.charts

import info.blockchain.api.data.Chart
import info.blockchain.api.data.Point
import info.blockchain.api.statistics.Statistics
import io.reactivex.Observable
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.util.annotations.WebRequest

class ChartsDataManager(private val statistics: Statistics, rxBus: RxBus) {

    private val rxPinning = RxPinning(rxBus)

    fun getYearPrice(): Observable<Point> = rxPinning.call<Point> {
        getWrappedCall(TimeSpan.YEAR)
                .flatMapIterable { it.values }
                .compose(RxUtil.applySchedulersToObservable())
    }

    fun getMonthPrice(): Observable<Point> = rxPinning.call<Point> {
        getWrappedCall(TimeSpan.MONTH)
                .flatMapIterable { it.values }
                .compose(RxUtil.applySchedulersToObservable())
    }

    fun getWeekPrice(): Observable<Point> = rxPinning.call<Point> {
        getWrappedCall(TimeSpan.WEEK)
                .flatMapIterable { it.values }
                .compose(RxUtil.applySchedulersToObservable())
    }


    @WebRequest
    private fun getWrappedCall(timeSpan: TimeSpan): Observable<Chart> = Observable.fromCallable {
        statistics.getChart(MARKET_PRICE, timeSpan.timeValue, AVERAGE_8_HOURS).execute().body()
    }

}