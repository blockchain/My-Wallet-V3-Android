package info.blockchain.wallet.shapeshift

import info.blockchain.wallet.shapeshift.data.MarketInfo
import info.blockchain.wallet.shapeshift.data.QuoteRequest
import info.blockchain.wallet.shapeshift.data.QuoteResponseWrapper
import info.blockchain.wallet.shapeshift.data.SendAmountResponseWrapper
import info.blockchain.wallet.shapeshift.data.TimeRemaining
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ShapeShiftEndpoints {

    @GET(ShapeShiftUrls.MARKET_INFO + "/{pair}")
    fun getMarketInfo(@Path("pair") pair: String): Observable<MarketInfo>

    @POST(ShapeShiftUrls.SENDAMOUNT)
    fun getQuote(@Body request: QuoteRequest): Observable<SendAmountResponseWrapper>

    @POST(ShapeShiftUrls.SENDAMOUNT)
    fun getApproximateQuote(@Body request: QuoteRequest): Observable<QuoteResponseWrapper>

    @GET(ShapeShiftUrls.TX_STATS + "/{address}")
    fun getTradeStatus(@Path("address") address: String): Observable<TradeStatusResponse>

    @GET(ShapeShiftUrls.TIME_REMAINING + "/{address}")
    fun getTimeRemaining(@Path("address") address: String): Observable<TimeRemaining>
}
