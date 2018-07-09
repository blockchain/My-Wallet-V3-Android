package piuk.blockchain.android.data.datamanagers;

import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.testutils.RxTest;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class QrCodeDataManagerTest extends RxTest {

    private QrCodeDataManager subject;
    private static final String TEST_URI = "bitcoin://1234567890";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        subject = new QrCodeDataManager();
    }

    @Test
    public void generateQrCode() throws Exception {
        // Arrange

        // Act
        TestObserver<Bitmap> observer = subject.generateQrCode(TEST_URI, 100).test();
        getTestScheduler().triggerActions();
        // Assert
        Bitmap bitmap = observer.values().get(0);
        assertNotNull(bitmap);
        assertEquals(100, bitmap.getWidth());
        assertEquals(100, bitmap.getHeight());
        observer.assertComplete();
        observer.assertNoErrors();
    }

    @Test
    public void generatePairingCode() throws Exception {
        // Arrange

        // Act
        TestObserver<Bitmap> observer = subject.generatePairingCode(
                UUID.randomUUID().toString(),
                "",
                UUID.randomUUID().toString(),
                "phrase",
                180).test();
        getTestScheduler().triggerActions();
        // Assert
        Bitmap bitmap = observer.values().get(0);
        assertNotNull(bitmap);
        assertEquals(180, bitmap.getWidth());
        assertEquals(180, bitmap.getHeight());
        observer.assertComplete();
        observer.assertNoErrors();
    }
}