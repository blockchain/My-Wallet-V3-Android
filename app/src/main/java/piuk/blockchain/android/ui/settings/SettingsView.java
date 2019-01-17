package piuk.blockchain.android.ui.settings;

import android.support.annotation.StringRes;

import com.blockchain.kyc.models.nabu.Kyc2TierState;
import com.blockchain.kycui.settings.SettingsKycState;
import piuk.blockchain.androidcoreui.ui.base.View;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;

interface SettingsView extends View {

    void setUpUi();

    void showFingerprintDialog(String pincode);

    void showDisableFingerprintDialog();

    void updateFingerprintPreferenceStatus();

    void showNoFingerprintsAddedDialog();

    void showProgressDialog(@StringRes int message);

    void hideProgressDialog();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void setGuidSummary(String summary);

    void setKycState(Kyc2TierState kycState);

    void setEmailSummary(String summary);

    void setSmsSummary(String summary);

    void setFiatSummary(String summary);

    void setEmailNotificationsVisibility(boolean visible);

    void setEmailNotificationPref(boolean enabled);

    void setPushNotificationPref(boolean enabled);

    void setFingerprintVisibility(boolean visible);

    void setTwoFaPreference(boolean enabled);

    void setTorBlocked(boolean blocked);

    void setScreenshotsEnabled(boolean enabled);

    void showDialogEmailVerification();

    void showDialogVerifySms();

    void showDialogSmsVerified();

    void goToPinEntryPage();

    void setLauncherShortcutVisibility(boolean visible);

    void showWarningDialog(@StringRes int message);

    void launchHomebrew(String defaultCurrency);

    void launchKycFlow();
}
