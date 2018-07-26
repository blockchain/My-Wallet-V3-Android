package info.blockchain.wallet.shapeshift;

import info.blockchain.wallet.shapeshift.data.MarketInfo;
import info.blockchain.wallet.shapeshift.data.QuoteRequest;
import info.blockchain.wallet.shapeshift.data.QuoteResponseWrapper;
import info.blockchain.wallet.shapeshift.data.SendAmountResponseWrapper;
import info.blockchain.wallet.shapeshift.data.TimeRemaining;
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ShapeShiftEndpoints {

    @GET(ShapeShiftUrls.MARKET_INFO + "/{pair}")
    Observable<MarketInfo> getMarketInfo(@Path("pair") String pair);

    @POST(ShapeShiftUrls.SENDAMOUNT)
    Observable<SendAmountResponseWrapper> getQuote(@Body QuoteRequest request);

    @POST(ShapeShiftUrls.SENDAMOUNT)
    Observable<QuoteResponseWrapper> getApproximateQuote(@Body QuoteRequest request);

    @GET(ShapeShiftUrls.TX_STATS + "/{address}")
    Observable<TradeStatusResponse> getTradeStatus(@Path("address") String address);

    @GET(ShapeShiftUrls.TIME_REMAINING + "/{address}")
    Observable<TimeRemaining> getTimeRemaining(@Path("address") String address);
}
