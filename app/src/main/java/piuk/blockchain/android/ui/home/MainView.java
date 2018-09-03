package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatDialogFragment;

import piuk.blockchain.androidbuysell.models.WebViewLoginDetails;
import piuk.blockchain.androidcoreui.ui.base.View;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;

interface MainView extends View {

    boolean isBuySellPermitted();

    void onScanInput(String strUri);

    void onStartContactsActivity(@Nullable String data);

    void onStartBalanceFragment(boolean paymentToContactMade);

    void kickToLauncherPage();

    void showProgressDialog(@StringRes int message);

    void hideProgressDialog();

    void clearAllDynamicShortcuts();

    void showMetadataNodeFailure();

    void setBuySellEnabled(boolean enabled, boolean useWebView);

    void onTradeCompleted(String txHash);

    void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails);

    void showCustomPrompt(AppCompatDialogFragment alertFragment);

    Context getActivityContext();

    void showSecondPasswordDialog();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void showExchange();

    void hideExchange();

    void showTestnetWarning();

    void onStartLegacyBuySell();

    void onStartBuySell();

    void showHomebrewDebug();
}
