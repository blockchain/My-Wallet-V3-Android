package info.blockchain.wallet.shapeshift;

import com.blockchain.morph.CoinPair;
import info.blockchain.wallet.shapeshift.data.MarketInfo;
import info.blockchain.wallet.shapeshift.data.QuoteRequest;
import info.blockchain.wallet.shapeshift.data.QuoteResponseWrapper;
import info.blockchain.wallet.shapeshift.data.SendAmountResponseWrapper;
import info.blockchain.wallet.shapeshift.data.TimeRemaining;
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse;
import io.reactivex.Observable;

public class ShapeShiftApi {

    private final ShapeShiftEndpoints shift;

    public ShapeShiftApi(ShapeShiftEndpoints shapeShiftEndpoints) {
        shift = shapeShiftEndpoints;
    }

    public Observable<MarketInfo> getRate(CoinPair coinPair) {
        return shift.getMarketInfo(coinPair.getPairCode());
    }

    public Observable<SendAmountResponseWrapper> getQuote(QuoteRequest request) {
        return shift.getQuote(request);
    }

    public Observable<QuoteResponseWrapper> getApproximateQuote(QuoteRequest request) {
        return shift.getApproximateQuote(request);
    }

    public Observable<TradeStatusResponse> getTradeStatus(String address) {
        return shift.getTradeStatus(address);
    }

    public Observable<TimeRemaining> getTimeRemaining(String address) {
        return shift.getTimeRemaining(address);
    }
}
