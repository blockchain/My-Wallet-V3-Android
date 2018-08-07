package info.blockchain.wallet.prices;

import info.blockchain.wallet.ApiCode;
import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.prices.data.PriceDatum;
import io.reactivex.observers.TestObserver;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PriceApiTest extends BaseIntegTest {

    private PriceApi subject = new PriceApi(
            BlockchainFramework.getRetrofitApiInstance().create(PriceEndpoints.class),
            new ApiCode() {
                @NotNull
                @Override
                public String getApiCode() {
                    return "integrationTest";
                }
            });

    @Test
    public void getHistoricPriceSeries() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -365);
        long oneYearAgo = cal.getTimeInMillis() / 1000;
        // Act
        final TestObserver<List<PriceDatum>> testObserver =
                subject.getHistoricPriceSeries("eth", "gbp", oneYearAgo, Scale.ONE_DAY).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(365, testObserver.values().get(0).size());
    }

    @Test
    public void getCurrentPrice() {
        // Arrange

        // Act
        final TestObserver<Double> testObserver =
                subject.getCurrentPrice("btc", "usd").test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void getHistoricPrice() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -365);
        long oneYearAgo = cal.getTimeInMillis() / 1000;
        // Act
        final TestObserver<Double> testObserver =
                subject.getHistoricPrice("btc", "usd", oneYearAgo).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void getPriceIndexes() {
        // Arrange

        // Act
        final TestObserver<Map<String, PriceDatum>> testObserver =
                subject.getPriceIndexes("ETH").test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

}