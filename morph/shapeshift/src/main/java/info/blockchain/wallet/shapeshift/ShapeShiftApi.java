package info.blockchain.wallet.shapeshift;

import info.blockchain.wallet.shapeshift.data.TradeStatusResponse;
import io.reactivex.Observable;

public class ShapeShiftApi {

    private final ShapeShiftEndpoints shift;

    public ShapeShiftApi(ShapeShiftEndpoints shapeShiftEndpoints) {
        shift = shapeShiftEndpoints;
    }

    public Observable<TradeStatusResponse> getTradeStatus(String address) {
        return shift.getTradeStatus(address);
    }
}
