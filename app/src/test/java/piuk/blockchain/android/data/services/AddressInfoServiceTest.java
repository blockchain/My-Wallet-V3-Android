package piuk.blockchain.android.data.services;

import info.blockchain.api.AddressInfo;
import info.blockchain.wallet.payload.LegacyAddress;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.data.services.AddressInfoService.PARAMETER_FINAL_BALANCE;

public class AddressInfoServiceTest extends RxTest {

    private AddressInfoService subject;
    @Mock AddressInfo addressInfo;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AddressInfoService(addressInfo);
    }

    @Test
    public void getAddressBalanceSuccess() throws Exception {
        // Arrange
        TestSubscriber<Long> subscriber = new TestSubscriber<>();
        LegacyAddress mockAddress = mock(LegacyAddress.class);
        when(mockAddress.getAddress()).thenReturn("1234567890");
        when(addressInfo.getAddressInfo(anyString(), anyString())).thenReturn((new JSONObject(VALID_JSON)));
        // Act
        subject.getAddressBalance(mockAddress, PARAMETER_FINAL_BALANCE).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(1000L, subscriber.getOnNextEvents().get(0).longValue());
    }

    private static final String VALID_JSON = "{\n" +
            "  \"final_balance\": 1000\n" +
            "}";
}