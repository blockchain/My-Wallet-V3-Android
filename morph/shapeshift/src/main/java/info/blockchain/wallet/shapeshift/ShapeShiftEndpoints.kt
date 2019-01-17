package info.blockchain.wallet.shapeshift

import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface ShapeShiftEndpoints {

    @GET(ShapeShiftUrls.TX_STATS + "/{address}")
    fun getTradeStatus(@Path("address") address: String): Observable<TradeStatusResponse>
}
