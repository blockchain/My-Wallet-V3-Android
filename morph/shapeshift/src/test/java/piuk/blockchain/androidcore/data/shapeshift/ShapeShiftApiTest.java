package piuk.blockchain.androidcore.data.shapeshift;

import info.blockchain.wallet.shapeshift.ShapeShiftApi;
import info.blockchain.wallet.shapeshift.ShapeShiftEndpoints;
import info.blockchain.wallet.shapeshift.ShapeShiftUrls;
import info.blockchain.wallet.shapeshift.data.Trade;
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse;
import io.reactivex.observers.TestObserver;
import org.junit.Test;
import retrofit2.Retrofit;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class ShapeShiftApiTest extends MockedResponseTest {

    private Retrofit getRetrofitShapeShiftInstance() {
        return getRetrofit(ShapeShiftUrls.SHAPESHIFT_URL, okHttpClient);
    }

    private ShapeShiftApi subject = new ShapeShiftApi(
            getRetrofitShapeShiftInstance().create(ShapeShiftEndpoints.class)
    );

    @Test
    public void getSendAmount() {
        mockInterceptor
                .setResponseString("{\n"
                        + "\t\"status\": \"complete\",\n"
                        + "\t\"address\": \"3PpfQbaETF1PCUh2iZKfMoyMhCmZWmVz9Z\",\n"
                        + "\t\"withdraw\": \"0x9240d92140a48164ef71d9b0fade096583354e5a\",\n"
                        + "\t\"incomingCoin\": 0.0001332,\n"
                        + "\t\"incomingType\": \"BTC\",\n"
                        + "\t\"outgoingCoin\": \"0.00099547\",\n"
                        + "\t\"outgoingType\": \"ETH\",\n"
                        + "\t\"transaction\": \"0xc1361e8ec096dfe48f524bd67fe811e5fd86a41c868ff5843f04619906882123\"\n"
                        + "}");
        mockInterceptor.setResponseCode(200);

        final TestObserver<TradeStatusResponse> testObserver = subject.getTradeStatus("someAddress").test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        TradeStatusResponse response = testObserver.values().get(0);
        assertEquals(Trade.STATUS.COMPLETE, response.getStatus());
        assertEquals("3PpfQbaETF1PCUh2iZKfMoyMhCmZWmVz9Z", response.getAddress());
        assertEquals("0x9240d92140a48164ef71d9b0fade096583354e5a", response.getWithdraw());
        assertEquals(BigDecimal.valueOf(0.0001332), response.getIncomingCoin());
        assertEquals("BTC", response.getIncomingType());
        assertEquals(BigDecimal.valueOf(0.00099547), response.getOutgoingCoin());
        assertEquals("ETH", response.getOutgoingType());
        assertEquals("0xc1361e8ec096dfe48f524bd67fe811e5fd86a41c868ff5843f04619906882123", response.getTransaction());
        assertEquals("BTC_ETH", response.getPair());
        assertNull(response.getError());
    }
}