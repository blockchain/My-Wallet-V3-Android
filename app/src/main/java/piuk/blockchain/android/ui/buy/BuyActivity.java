package piuk.blockchain.android.ui.buy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;

import com.facebook.device.yearclass.YearClass;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.databinding.ActivityBuyBinding;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.util.AndroidUtils;

public class BuyActivity extends BaseAuthActivity implements BuyViewModel.DataListener, FrontendJavascript<String> {

    public static final String TAG = BuyActivity.class.getSimpleName();
    public static final String WEB_VIEW_STATE_KEY = "web_view_state";

    private FrontendJavascriptManager frontendJavascriptManager;
    private WebViewLoginDetails webViewLoginDetails;
    private BuyViewModel viewModel;
    private MaterialProgressDialog progress;
    private ActivityBuyBinding binding;

    private boolean frontendInitialized = false;
    private boolean didBuyBitcoin = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_buy);

        setupToolbar(binding.toolbarContainer.toolbarGeneral, R.string.onboarding_buy_bitcoin);
        viewModel = new BuyViewModel(this);

        showProgressDialog();

        if (AndroidUtils.is21orHigher()) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true);
        }

        frontendJavascriptManager = new FrontendJavascriptManager(this, binding.webview);

        binding.webview.addJavascriptInterface(frontendJavascriptManager, FrontendJavascriptManager.JS_INTERFACE_NAME);
        binding.webview.getSettings().setJavaScriptEnabled(true);
        binding.webview.restoreState(getIntent().getParcelableExtra(WEB_VIEW_STATE_KEY));
        viewModel.onViewReady();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        frontendJavascriptManager.teardown();
        binding.webview.removeJavascriptInterface(FrontendJavascriptManager.JS_INTERFACE_NAME);

        if (didBuyBitcoin) {
            viewModel.reloadExchangeDate();
        }
        dismissProgressDialog();
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "Received JS value: " + value);
    }

    public void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails) {
        Log.d(TAG, "setWebViewLoginDetails: done");
        this.webViewLoginDetails = webViewLoginDetails;
        activateIfReady();
    }

    @Override
    public void onFrontendInitialized() {
        Log.d(TAG, "onFrontendInitialized: done");
        frontendInitialized = true;
        activateIfReady();
    }

    @Override
    public void onBuyCompleted() {
        Log.d(TAG, "onBuyCompleted: done");
        didBuyBitcoin = true;
    }

    @Override
    public void onShowTx(String txHash) {
        Bundle bundle = new Bundle();
        bundle.putString(BalanceFragment.KEY_TRANSACTION_HASH, txHash);
        TransactionDetailActivity.start(this, bundle);
    }

    private void activateIfReady() {
        if (isReady()) {
            frontendJavascriptManager.activateMobileBuyFromJson(
                    webViewLoginDetails,
                    viewModel.isNewlyCreated());
            dismissProgressDialog();
        }
    }

    public boolean isReady() {
        return frontendInitialized && webViewLoginDetails != null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Handled in {@link MainActivity}
     */
    @Override
    public void onCompletedTrade(String txHash) {
        // No-op
    }

    public void showProgressDialog() {

        int message = R.string.please_wait;

        int year = YearClass.get(this);
        if (year < 2013) {
            // Phone too slow, show performance warning
            message = R.string.onboarding_buy_performance_warning;
        }

        dismissProgressDialog();
        if (!isFinishing()) {
            progress = new MaterialProgressDialog(this);
            progress.setMessage(message);
            progress.setCancelable(false);
            progress.show();
        }
    }

    public void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }

    public static void start(Context context, Bundle webViewState) {
        Intent starter = new Intent(context, BuyActivity.class);
        starter.putExtra(WEB_VIEW_STATE_KEY, webViewState);
        context.startActivity(starter);
    }
}
