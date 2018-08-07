package info.blockchain.wallet.prices;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.ApiCode;
import info.blockchain.wallet.prices.data.PriceDatum;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @see <a href=https://api.blockchain.info/price/specs>Blockchain Price API specs</a>
 */
public class PriceApi implements CurrentPriceApi {

    private final PriceEndpoints endpoints;
    private final ApiCode apiCode;

    public PriceApi(PriceEndpoints endpoints, ApiCode apiCode) {
        this.endpoints = endpoints;
        this.apiCode = apiCode;
    }

    /**
     * Returns a {@link List} of {@link PriceDatum} objects, containing a timestamp and a price for
     * that given time.
     *
     * @param base  The base cryptocurrency for which to gather prices, eg "eth", "btc" or "bcc"
     * @param quote The fiat currency in which to return the prices, eg "usd"
     * @param start The start time, in epoch seconds, from which to gather historic data
     * @param scale The scale which you want to use between price data, eg {@link Scale#ONE_DAY}
     * @return An {@link Observable} wrapping a {@link List} of {@link PriceDatum} objects
     * @see Scale
     */
    public Observable<List<PriceDatum>> getHistoricPriceSeries(String base,
                                                               String quote,
                                                               long start,
                                                               int scale) {
        return endpoints.getHistoricPriceSeries(base,
                quote,
                start,
                scale,
                apiCode.getApiCode());
    }

    /**
     * Provides the exchange rate between a cryptocurrency and a fiat currency for this moment in
     * time. Returns a single {@link PriceDatum} object.
     *
     * @param base  The base cryptocurrency for which to gather prices, eg "eth", "btc" or "bcc"
     * @param quote The fiat currency in which to return the price, eg "usd"
     * @return An {@link Observable} wrapping a {@link PriceDatum} object
     */
    public Observable<Double> getCurrentPrice(String base,
                                              String quote) {
        return getCurrentPriceDatum(base, quote)
                .map(new Function<PriceDatum, Double>() {
                    @Override
                    public Double apply(PriceDatum priceDatum) throws Exception {
                        return priceDatum.getPrice();
                    }
                });
    }

    private Observable<PriceDatum> getCurrentPriceDatum(String base, String quote) {
        return endpoints.getCurrentPrice(base,
                quote,
                apiCode.getApiCode());
    }

    /**
     * Provides the exchange rate between a cryptocurrency and a fiat currency for a given moment in
     * time, supplied in seconds since epoch. Returns a single {@link PriceDatum} object.
     *
     * @param base  The base cryptocurrency for which to gather prices, eg "eth", "btc" or "bcc"
     * @param quote The fiat currency in which to return the price, eg "usd"
     * @param time  The time in seconds since epoch for which you want to return a price
     * @return An {@link Observable} wrapping a {@link PriceDatum} object
     */
    public Observable<Double> getHistoricPrice(String base,
                                               String quote,
                                               long time) {
        return endpoints.getHistoricPrice(base,
                quote,
                time,
                apiCode.getApiCode())
                .map(new Function<PriceDatum, Double>() {
                    @Override
                    public Double apply(PriceDatum priceDatum) throws Exception {
                        return priceDatum.getPrice();
                    }
                });
    }

    /**
     * Provides a {@link Map} of currency codes to current {@link PriceDatum} objects for a given
     * base cryptocurrency. For instance, getting "USD" would return the current price, timestamp
     * and volume in an object. This is a direct replacement for the Ticker.
     *
     * @param base The base cryptocurrency that you want prices for, eg. ETH
     * @return A {@link Map} of {@link PriceDatum} objects.
     */
    public Observable<Map<String, PriceDatum>> getPriceIndexes(String base) {
        return endpoints.getPriceIndexes(base, apiCode.getApiCode());
    }

    @NotNull
    @Override
    public Single<BigDecimal> currentPrice(@NotNull CryptoCurrency base, @NotNull String quoteFiatCode) {
        return getCurrentPriceDatum(base.getSymbol(), quoteFiatCode)
                .map(new Function<PriceDatum, BigDecimal>() {
                    @Override
                    public BigDecimal apply(PriceDatum priceDatum) {
                        return BigDecimal.valueOf(priceDatum.getPrice());
                    }
                })
                .singleOrError();
    }
}
