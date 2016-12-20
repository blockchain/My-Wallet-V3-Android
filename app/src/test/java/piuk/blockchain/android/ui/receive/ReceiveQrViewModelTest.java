package piuk.blockchain.android.ui.receive;

import android.content.Intent;
import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.datamanagers.ReceiveDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import io.reactivex.Observable;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class ReceiveQrViewModelTest {

    private ReceiveQrViewModel subject;
    @Mock ReceiveQrViewModel.DataListener activity;
    @Mock ReceiveDataManager receiveDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new ApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule()
        );

        subject = new ReceiveQrViewModel(activity);
    }

    @Test
    public void onViewReadyWithIntent() throws Exception {
        // Arrange
        Intent intent = new Intent();
        String address = "address";
        String label = "label";
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, address);
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, label);
        when(activity.getPageIntent()).thenReturn(intent);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
        when(receiveDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(bitmap));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setAddressInfo(address);
        verify(activity).setAddressLabel(label);
        verify(activity).setImageBitmap(bitmap);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyWithIntentBitmapException() throws Exception {
        // Arrange
        Intent intent = new Intent();
        String address = "address";
        String label = "label";
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, address);
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, label);
        when(activity.getPageIntent()).thenReturn(intent);
        when(receiveDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setAddressInfo(address);
        verify(activity).setAddressLabel(label);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).finishActivity();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyNoIntent() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(null);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).finishActivity();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onCopyClicked() throws Exception {
        // Arrange
        String address = "address";
        subject.receiveAddressString = address;
        // Act
        subject.onCopyClicked();
        // Assert
        verify(activity).showClipboardWarning(address);
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected ReceiveDataManager provideReceiveDataManager() {
            return receiveDataManager;
        }
    }
}