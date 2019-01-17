package piuk.blockchain.android.ui.dashboard

import io.reactivex.Observable

enum class BalanceFilter {
    Total,
    Wallet,
    ColdStorage
}

interface DashboardData {

    fun getPieChartData(balanceFilter: Observable<BalanceFilter>): Observable<PieChartsState.Data>
}
