package info.blockchain.wallet.prices;

import info.blockchain.wallet.prices.data.PriceDatum;
import io.reactivex.Observable;
import java.util.List;
import java.util.Map;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PriceEndpoints {

    @GET(PriceUrls.PRICE_SERIES)
    Observable<List<PriceDatum>> getHistoricPriceSeries(@Query("base") String base,
                                                        @Query("quote") String quote,
                                                        @Query("start") long start,
                                                        @Query("scale") int scale,
                                                        @Query("api_key") String apiKey);

    @GET(PriceUrls.SINGLE_PRICE)
    Observable<PriceDatum> getCurrentPrice(@Query("base") String base,
                                           @Query("quote") String quote,
                                           @Query("api_key") String apiKey);

    @GET(PriceUrls.SINGLE_PRICE)
    Observable<PriceDatum> getHistoricPrice(@Query("base") String base,
                                            @Query("quote") String quote,
                                            @Query("time") long time,
                                            @Query("api_key") String apiKey);

    @GET(PriceUrls.PRICE_INDEXES)
    Observable<Map<String, PriceDatum>> getPriceIndexes(@Query("base") String base,
                                                        @Query("api_key") String apiKey);

}
