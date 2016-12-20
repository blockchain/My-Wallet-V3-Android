package piuk.blockchain.android.ui.fingerprint;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.support.annotation.RequiresApi;

import com.mtramin.rxfingerprint.data.FingerprintAuthenticationResult;
import com.mtramin.rxfingerprint.data.FingerprintDecryptionResult;
import com.mtramin.rxfingerprint.data.FingerprintEncryptionResult;
import com.mtramin.rxfingerprint.data.FingerprintResult;

import info.blockchain.wallet.util.CharSequenceX;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.data.fingerprint.FingerprintAuth;
import piuk.blockchain.android.util.PrefsUtil;
import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FingerprintHelperTest {

    private FingerprintHelper subject;
    @Mock Context applicationContext;
    @Mock PrefsUtil prefsUtil;
    @Mock FingerprintAuth fingerprintAuth;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new FingerprintHelper(applicationContext, prefsUtil, fingerprintAuth);
    }

    @Test
    public void isFingerprintAvailable() throws Exception {
        // Arrange
        when(fingerprintAuth.isFingerprintAvailable(applicationContext)).thenReturn(true);
        // Act
        boolean value = subject.isFingerprintAvailable();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void isHardwareDetected() throws Exception {
        // Arrange
        when(fingerprintAuth.isHardwareDetected(applicationContext)).thenReturn(true);
        // Act
        boolean value = subject.isHardwareDetected();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void areFingerprintsEnrolled() throws Exception {
        // Arrange
        when(fingerprintAuth.areFingerprintsEnrolled(applicationContext)).thenReturn(true);
        // Act
        boolean value = subject.areFingerprintsEnrolled();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void getIfFingerprintUnlockEnabledTrue() throws Exception {
        // Arrange
        when(fingerprintAuth.isFingerprintAvailable(applicationContext)).thenReturn(true);
        when(prefsUtil.getValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, false)).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintUnlockEnabled();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void getIfFingerprintUnlockEnabledFalse() throws Exception {
        // Arrange
        when(fingerprintAuth.isFingerprintAvailable(applicationContext)).thenReturn(true);
        when(prefsUtil.getValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, false)).thenReturn(false);
        // Act
        boolean value = subject.getIfFingerprintUnlockEnabled();
        // Assert
        assertEquals(false, value);
    }

    @Test
    public void setFingerprintUnlockEnabled() throws Exception {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(true);
        // Assert
        verify(prefsUtil).setValue(PrefsUtil.KEY_FINGERPRINT_ENABLED, true);
    }

    @Ignore
    @Test
    public void storeEncryptedData() throws Exception {
        /**
         * Not currently testable, Base64 not mocked
         */
    }

    @Ignore
    @Test
    public void getEncryptedData() throws Exception {
        /**
         * Not currently testable, Base64 not mocked
         */
    }

    @Test
    public void clearEncryptedData() throws Exception {
        // Arrange
        String key = "key";
        // Act
        subject.clearEncryptedData(key);
        // Assert
        verify(prefsUtil).removeValue(key);
    }

    @Test
    public void authenticateFingerprintFailed() throws Exception {
        // Arrange
        FingerprintAuthenticationResult result = new FingerprintAuthenticationResult(FingerprintResult.FAILED, "");
        when(fingerprintAuth.authenticate(applicationContext)).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.authenticateFingerprint(mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onFailure();
    }

    @Test
    public void authenticateFingerprintOnHelp() throws Exception {
        // Arrange
        String message = "help";
        FingerprintAuthenticationResult result = new FingerprintAuthenticationResult(FingerprintResult.HELP, message);
        when(fingerprintAuth.authenticate(applicationContext)).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.authenticateFingerprint(mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onHelp(message);
    }

    @Test
    public void authenticateFingerprintOnAuthenticated() throws Exception {
        // Arrange
        FingerprintAuthenticationResult result = new FingerprintAuthenticationResult(FingerprintResult.AUTHENTICATED, "");
        when(fingerprintAuth.authenticate(applicationContext)).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.authenticateFingerprint(mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onAuthenticated(null);
    }

    @Test
    public void authenticateFingerprintException() throws Exception {
        // Arrange
        when(fingerprintAuth.authenticate(applicationContext)).thenReturn(Observable.error(new Throwable()));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.authenticateFingerprint(mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onFatalError();
    }

    @Test
    public void encryptStringFailed() throws Exception {
        // Arrange
        FingerprintEncryptionResult result = new FingerprintEncryptionResult(FingerprintResult.FAILED, "", "");
        when(fingerprintAuth.encrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.encryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onFailure();
    }

    @Test
    public void encryptStringOnHelp() throws Exception {
        // Arrange
        String message = "help";
        FingerprintEncryptionResult result = new FingerprintEncryptionResult(FingerprintResult.HELP, message, "");
        when(fingerprintAuth.encrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.encryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onHelp(message);
    }

    @Test
    public void encryptStringOnAuthenticated() throws Exception {
        // Arrange
        FingerprintEncryptionResult result = new FingerprintEncryptionResult(FingerprintResult.AUTHENTICATED, "", "");
        when(fingerprintAuth.encrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.encryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onAuthenticated(any(CharSequenceX.class));
    }

    @Test
    public void encryptStringException() throws Exception {
        // Arrange
        when(fingerprintAuth.encrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.encryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onFatalError();
    }

    @Test
    public void decryptStringFailed() throws Exception {
        // Arrange
        FingerprintDecryptionResult result = new FingerprintDecryptionResult(FingerprintResult.FAILED, "", "");
        when(fingerprintAuth.decrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.decryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onFailure();
    }

    @Test
    public void decryptStringOnHelp() throws Exception {
        // Arrange
        String message = "help";
        FingerprintDecryptionResult result = new FingerprintDecryptionResult(FingerprintResult.HELP, message, "");
        when(fingerprintAuth.decrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.decryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onHelp(message);
    }

    @Test
    public void decryptStringOnAuthenticated() throws Exception {
        // Arrange
        FingerprintDecryptionResult result = new FingerprintDecryptionResult(FingerprintResult.AUTHENTICATED, "", "");
        when(fingerprintAuth.decrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.just(result));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.decryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onAuthenticated(any(CharSequenceX.class));
    }

    @Test
    public void decryptStringException() throws Exception {
        // Arrange
        when(fingerprintAuth.decrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.decryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onFatalError();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Test
    public void decryptStringKeyPermanentlyInvalidatedException() throws Exception {
        // Arrange
        when(fingerprintAuth.decrypt(eq(applicationContext), anyString(), anyString())).thenReturn(Observable.error(new KeyPermanentlyInvalidatedException()));
        FingerprintHelper.AuthCallback mockAuthCallback = mock(FingerprintHelper.AuthCallback.class);
        // Act
        subject.decryptString("", "", mockAuthCallback);
        // Assert
        verify(mockAuthCallback).onKeyInvalidated();
    }

    @Test
    public void releaseFingerprintReader() throws Exception {
        // Arrange

        // Act
        subject.releaseFingerprintReader();
        // Assert
        assertEquals(0, subject.compositeDisposable.size());
    }

}