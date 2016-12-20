package piuk.blockchain.android.ui.recover;

import android.app.Application;
import android.content.Intent;

import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class RecoverFundsViewModelTest {

    private RecoverFundsViewModel mSubject;

    @Mock private RecoverFundsActivity mActivity;
    @Mock private AuthDataManager mAuthDataManager;
    @Mock private PayloadManager mPayloadManager;
    @Mock private AppUtil mAppUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        mSubject = new RecoverFundsViewModel(mActivity);
    }

    /**
     * Recovery phrase missing, should inform user.
     */
    @Test
    public void onContinueClickedNoRecoveryPhrase() throws Exception {
        // Arrange
        when(mActivity.getRecoveryPhrase()).thenReturn(null);
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Recovery phrase is too short to be valid, should inform user.
     */
    @Test
    public void onContinueClickedInvalidRecoveryPhraseLength() throws Exception {
        // Arrange
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four");
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Password is missing in intent, something has gone wrong here. Should restart the app after
     * informing the user.
     */
    @Test
    public void onContinueClickedNoPasswordInIntent() throws Exception {
        // Arrange
        when(mActivity.getPageIntent()).thenReturn(mock(Intent.class));
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four five six seven eight nine ten eleven twelve");
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        verify(mActivity).getPageIntent();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).clearCredentialsAndRestart();
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Email is missing in intent, something has gone wrong here. Should restart the app after
     * informing the user.
     */
    @Test
    public void onContinueClickedNoEmailInIntent() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_PASSWORD, "password");
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four five six seven eight nine ten eleven twelve");
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        verify(mActivity, times(2)).getPageIntent();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).clearCredentialsAndRestart();
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Successful restore. Should take the user to the PIN entry page.
     */
    @Test
    public void onContinueClickedSuccessfulRestore() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_PASSWORD, "password");
        intent.putExtra(KEY_INTENT_EMAIL, "email");
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four five six seven eight nine ten eleven twelve");
        when(mAuthDataManager.restoreHdWallet(anyString(), anyString(), anyString())).thenReturn(Observable.just(new Payload()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        verify(mActivity, times(2)).getPageIntent();
        verify(mActivity).showProgressDialog(anyInt());
        verify(mActivity).dismissProgressDialog();
        verify(mActivity).goToPinEntryPage();
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Restore failed, inform the user.
     */
    @Test
    public void onContinueClickedRestoreFailed() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_PASSWORD, "password");
        intent.putExtra(KEY_INTENT_EMAIL, "email");
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four five six seven eight nine ten eleven twelve");
        when(mAuthDataManager.restoreHdWallet(anyString(), anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        verify(mActivity, times(2)).getPageIntent();
        verify(mActivity).showProgressDialog(anyInt());
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        mSubject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        assertEquals(util, mAppUtil);
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }
    }

    private class MockApiModule extends ApiModule {

        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected AuthDataManager provideAuthDataManager(PayloadManager payloadManager,
                                                         PrefsUtil prefsUtil,
                                                         AppUtil appUtil,
                                                         AESUtilWrapper aesUtilWrapper,
                                                         AccessState accessState,
                                                         StringUtils stringUtils) {
            return mAuthDataManager;
        }
    }

}