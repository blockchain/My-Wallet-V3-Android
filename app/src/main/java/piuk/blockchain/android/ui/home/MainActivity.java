package piuk.blockchain.android.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;

import org.jetbrains.annotations.NotNull;

import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import java.util.Arrays;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.contacts.models.PaymentRequestType;
import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.EventService;
import piuk.blockchain.android.databinding.ActivityMainBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.AccountActivity;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.backup.BackupWalletActivity;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.buy.BuyActivity;
import piuk.blockchain.android.ui.buy.FrontendJavascript;
import piuk.blockchain.android.ui.buy.FrontendJavascriptManager;
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.contacts.payments.ContactConfirmRequestFragment;
import piuk.blockchain.android.ui.contacts.success.ContactRequestSuccessFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.dashboard.DashboardFragment;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.ui.pairing_code.PairingCodeActivity;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.send.SendFragmentNew;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.EXTRA_METADATA_URI;

public class MainActivity extends BaseMvpActivity<MainView, MainPresenter> implements
        BalanceFragment.OnFragmentInteractionListener,
        MainView,
        SendFragmentNew.OnSendFragmentInteractionListener,
        ReceiveFragment.OnReceiveFragmentInteractionListener,
        ContactConfirmRequestFragment.FragmentInteractionListener,
        FrontendJavascript<String>,
        ConfirmPaymentDialog.OnConfirmDialogInteractionListener,
        ContactRequestSuccessFragment.ContactsRequestSuccessListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String ACTION_SEND = "info.blockchain.wallet.ui.BalanceFragment.SEND";
    public static final String ACTION_RECEIVE = "info.blockchain.wallet.ui.BalanceFragment.RECEIVE";
    public static final String ACTION_BUY = "info.blockchain.wallet.ui.BalanceFragment.BUY";

    private static final String SUPPORT_URI = "https://support.blockchain.com/";
    private static final int REQUEST_BACKUP = 2225;
    private static final int MERCHANT_ACTIVITY = 1;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    public static final String EXTRA_URI = "transaction_uri";
    public static final String EXTRA_RECIPIENT_ID = "recipient_id";
    public static final String EXTRA_MDID = "mdid";
    public static final String EXTRA_FCTX_ID = "fctx_id";

    public static final int SCAN_URI = 2007;
    public static final int ACCOUNT_EDIT = 2008;
    public static final int SETTINGS_EDIT = 2009;
    public static final int CONTACTS_EDIT = 2010;

    @Thunk boolean drawerIsOpen = false;

    @Inject MainPresenter mainPresenter;
    @Thunk ActivityMainBinding binding;
    private MaterialProgressDialog materialProgressDialog;
    private AppUtil appUtil;
    private long backPressed;
    private Toolbar toolbar;
    private boolean paymentMade = false;
    private Typeface typeface;
    private BalanceFragment balanceFragment;
    private FrontendJavascriptManager frontendJavascriptManager;
    private WebViewLoginDetails webViewLoginDetails;
    private boolean initialized;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(ACTION_SEND) && getActivity() != null) {
                requestScan();
            } else if (intent.getAction().equals(ACTION_RECEIVE) && getActivity() != null) {
                binding.bottomNavigation.setCurrentItem(3);
            } else if (intent.getAction().equals(ACTION_BUY) && getActivity() != null) {
                BuyActivity.start(MainActivity.this);
            }
        }
    };

    private AHBottomNavigation.OnTabSelectedListener tabSelectedListener = (position, wasSelected) -> {
        if (!wasSelected) {
            switch (position) {
                case 0:
                    if (!(getCurrentFragment() instanceof SendFragmentNew)) {
                        // This is a bit of a hack to allow the selection of the correct button
                        // On the bottom nav bar, but without starting the fragment again
                        startSendFragment(null, null);
                    }
                    break;
                case 1:
                    startDashboardFragment();
                    break;
                case 2:
                    onStartBalanceFragment(paymentMade);
                    break;
                case 3:
                    startReceiveFragment();
                    break;
            }
        }

        return true;
    };

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        IntentFilter filterSend = new IntentFilter(ACTION_SEND);
        IntentFilter filterReceive = new IntentFilter(ACTION_RECEIVE);
        IntentFilter filterBuy = new IntentFilter(ACTION_BUY);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterSend);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterReceive);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterBuy);

        appUtil = new AppUtil(this);
        balanceFragment = BalanceFragment.newInstance(false);

        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // No-op
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                drawerIsOpen = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                drawerIsOpen = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // No-op
            }
        });

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar_general);
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.vector_menu));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        ViewUtils.setElevation(toolbar, 0F);

        // Notify Presenter that page is setup
        onViewReady();

        // Create items
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.send_bitcoin, R.drawable.vector_send, R.color.white);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.dashboard_title, R.drawable.vector_home, R.color.white);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(R.string.overview, R.drawable.vector_transactions, R.color.white);
        AHBottomNavigationItem item4 = new AHBottomNavigationItem(R.string.receive_bitcoin, R.drawable.vector_receive, R.color.white);

        // Add items
        binding.bottomNavigation.addItems(Arrays.asList(item1, item2, item3, item4));

        // Styling
        binding.bottomNavigation.setAccentColor(ContextCompat.getColor(this, R.color.primary_blue_accent));
        binding.bottomNavigation.setInactiveColor(ContextCompat.getColor(this, R.color.primary_gray_dark));
        binding.bottomNavigation.setForceTint(true);
        binding.bottomNavigation.setUseElevation(true);
        Typeface typeface = TypefaceUtils.load(getAssets(), "fonts/Montserrat-Regular.ttf");
        binding.bottomNavigation.setTitleTypeface(typeface);

        // Select Dashboard by default
        binding.bottomNavigation.setOnTabSelectedListener(tabSelectedListener);
        binding.bottomNavigation.setCurrentItem(1);

        handleIncomingIntent();
        applyFontToNavDrawer();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        appUtil.deleteQR();
        getPresenter().updateTicker();
        resetNavigationDrawer();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                binding.drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_qr_main:
                requestScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void start(Context context, Bundle bundle) {
        Intent starter = new Intent(context, MainActivity.class);
        starter.putExtras(bundle);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    public void setMessagesCount(int messageCount) {
        if (messageCount > 0) {
            AHNotification notification = new AHNotification.Builder()
                    .setText(String.valueOf(messageCount))
                    .setBackgroundColor(ContextCompat.getColor(this, R.color.product_red_medium))
                    .setTextColor(ContextCompat.getColor(this, R.color.white))
                    .build();
            binding.bottomNavigation.setNotification(notification, 1);
        } else {
            binding.bottomNavigation.setNotification(new AHNotification(), 1);
        }
    }

    @Thunk
    Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    public boolean getDrawerOpen() {
        return drawerIsOpen;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            doScanInput(strResult, EventService.EVENT_TX_INPUT_FROM_QR);

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetNavigationDrawer();
        } else if (requestCode == SETTINGS_EDIT || requestCode == CONTACTS_EDIT) {
            // Reset state incase of changing currency etc
            binding.bottomNavigation.setCurrentItem(1);
            // Pass this result to balance fragment
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerIsOpen) {
            binding.drawerLayout.closeDrawers();
        } else if (getCurrentFragment() instanceof BalanceFragment) {
            handleBackPressed();
        } else if (getCurrentFragment() instanceof SendFragmentNew) {
            ((SendFragmentNew) getCurrentFragment()).onBackPressed();
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            ((ReceiveFragment) getCurrentFragment()).onBackPressed();
        } else //noinspection StatementWithEmptyBody
            if (getCurrentFragment() instanceof DashboardFragment) {
            // Ignore
        } else if (getCurrentFragment() instanceof ContactConfirmRequestFragment) {
            // Remove Notes fragment from stack
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().remove(getCurrentFragment()).commit();
        } else {
            // Switch to balance fragment
            balanceFragment = BalanceFragment.newInstance(false);
            replaceFragmentWithAnimation(balanceFragment);
        }
    }

    public void handleBackPressed() {
        if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(this);
            return;
        } else {
            onExitConfirmToast();
        }

        backPressed = System.currentTimeMillis();
    }

    public void onExitConfirmToast() {
        ToastCustom.makeText(getActivity(), getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    }

    @Thunk
    void startScanActivity() {
        if (!appUtil.isCameraOpen()) {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_URI);
        } else {
            ToastCustom.makeText(MainActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void doScanInput(String strResult, String scanRoute) {
        startSendFragment(strResult, scanRoute);
    }

    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_backup:
                startActivityForResult(new Intent(this, BackupWalletActivity.class), REQUEST_BACKUP);
                break;
            case R.id.nav_addresses:
                startActivityForResult(new Intent(this, AccountActivity.class), ACCOUNT_EDIT);
                break;
            case R.id.nav_buy:
                BuyActivity.start(this);
                break;
            case R.id.nav_contacts:
                startActivityForResult(new Intent(this, ContactsListActivity.class), CONTACTS_EDIT);
                break;
            case R.id.login_web_wallet:
                PairingCodeActivity.start(this);
                break;
            case R.id.nav_map:
                startMerchantActivity();
                break;
            case R.id.nav_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_EDIT);
                break;
            case R.id.nav_support:
                onSupportClicked();
                break;
            case R.id.nav_logout:
                new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                        .setTitle(R.string.unpair_wallet)
                        .setMessage(R.string.ask_you_sure_unpair)
                        .setPositiveButton(R.string.unpair, (dialog, which) -> getPresenter().unPair())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
        }
        binding.drawerLayout.closeDrawers();
    }

    private void onSupportClicked() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.support_leaving_app_warning)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URI))))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void resetNavigationDrawer() {
        // Called onResume from BalanceFragment
        toolbar.setTitle("");

        binding.navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });

        // Set selected appropriately.
        if (getCurrentFragment() instanceof BalanceFragment) {
            binding.bottomNavigation.setCurrentItem(1);
        } else if (getCurrentFragment() instanceof SendFragmentNew) {
            binding.bottomNavigation.setCurrentItem(0);
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            binding.bottomNavigation.setCurrentItem(2);
        }

        if (!BuildConfig.CONTACTS_ENABLED) {
            MenuItem contactsMenuItem = binding.navigationView.getMenu().findItem(R.id.nav_contacts);
            contactsMenuItem.setVisible(false);
        }
    }

    private void startMerchantActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestLocationPermissionFromActivity(binding.getRoot(), this);
        } else {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!enabled) {
                String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setMessage(getActivity().getString(R.string.enable_geo))
                        .setPositiveButton(android.R.string.ok, (d, id) ->
                                getActivity().startActivity(new Intent(action)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
            } else {
                Intent intent = new Intent(MainActivity.this, piuk.blockchain.android.ui.directory.MapActivity.class);
                startActivityForResult(intent, MERCHANT_ACTIVITY);
            }
        }
    }

    @Thunk
    void requestScan() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), MainActivity.this);
        } else {
            startScanActivity();
        }
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity();
            } else {
                // Permission request was denied.
            }
        }
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startMerchantActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Thunk
    Context getActivity() {
        return this;
    }

    @Override
    public void showMetadataNodeRegistrationFailure() {
        if (!isFinishing()) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.contacts_register_nodes_failure)
                    .setPositiveButton(R.string.retry, (dialog, which) -> getPresenter().checkForMessages())
                    .create()
                    .show();
        }
    }

    @Override
    public void onPaymentInitiated(@NonNull String uri, @NonNull String recipientId, @NonNull String mdid, @NonNull String fctxId) {
        startContactSendDialog(uri, recipientId, mdid, fctxId);
    }

    @Override
    public void kickToLauncherPage() {
        startSingleActivity(LauncherActivity.class);
    }

    @Override
    public void onStartContactsActivity(@Nullable String data) {
        if (data != null) {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_METADATA_URI, data);
            ContactsListActivity.start(this, bundle);
        } else {
            ContactsListActivity.start(this, null);
        }
    }

    @Override
    public void showProgressDialog(@StringRes int message) {
        hideProgressDialog();
        materialProgressDialog = new MaterialProgressDialog(this);
        materialProgressDialog.setCancelable(false);
        materialProgressDialog.setMessage(message);
        materialProgressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (materialProgressDialog != null) {
            materialProgressDialog.dismiss();
            materialProgressDialog = null;
        }
    }

    @Override
    public void onScanInput(String strUri) {
        doScanInput(strUri, EventService.EVENT_TX_INPUT_FROM_URI);
    }

    @Override
    public void onStartBalanceFragment(boolean paymentToContactMade) {
        if (paymentToContactMade) {
            balanceFragment = BalanceFragment.newInstance(true);
            paymentMade = false;
        }
        replaceFragmentWithAnimation(balanceFragment);
        toolbar.setTitle("");
    }

    public AHBottomNavigation getBottomNavigationView() {
        return binding.bottomNavigation;
    }

    private void applyFontToNavDrawer() {
        Menu menu = binding.navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            applyFontToMenuItem(menuItem);
        }
    }

    public void restoreBottomNavigation() {
        binding.bottomNavigation.restoreBottomNavigation(true);
    }

    public void hideBottomNavigation() {
        binding.bottomNavigation.hideBottomNavigation(true);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean isBuySellPermitted() {
        return AndroidUtils.is19orHigher();
    }

    @Override
    public void setBuySellEnabled(boolean enabled) {
        if (enabled) {
            setupBuyWebView();
        }
        setBuyBitcoinVisible(enabled);
    }

    @Override
    public void onTradeCompleted(String txHash) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.trade_complete))
                .setMessage(R.string.trade_complete_details)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_cap, null)
                .setNegativeButton(R.string.view_details, (dialog, whichButton) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(BalanceFragment.KEY_TRANSACTION_HASH, txHash);
                    TransactionDetailActivity.start(this, bundle);
                }).show();
    }

    private void setBuyBitcoinVisible(boolean visible) {
        Menu menu = binding.navigationView.getMenu();
        menu.findItem(R.id.nav_buy).setVisible(visible);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupBuyWebView() {
        if (AndroidUtils.is21orHigher()) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
        // Setup buy WebView
        WebView buyWebView = new WebView(this);
        buyWebView.setWebViewClient(new WebViewClient());
        buyWebView.getSettings().setJavaScriptEnabled(true);
        buyWebView.loadUrl(getPresenter().getCurrentServerUrl() + "wallet/#/intermediate");

        frontendJavascriptManager = new FrontendJavascriptManager(this, buyWebView);
        buyWebView.addJavascriptInterface(frontendJavascriptManager, FrontendJavascriptManager.JS_INTERFACE_NAME);
    }

    private void checkTradesIfReady() {
        if (initialized && webViewLoginDetails != null) {
            frontendJavascriptManager.checkForCompletedTrades(webViewLoginDetails);
        }
    }

    public void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails) {
        Log.d(TAG, "setWebViewLoginDetails: called");
        this.webViewLoginDetails = webViewLoginDetails;
        checkTradesIfReady();
    }

    @Override
    public void onFrontendInitialized() {
        Log.d(TAG, "onFrontendInitialized: called");
        initialized = true;
        checkTradesIfReady();
    }

    @Override
    public void onBuyCompleted() {
        // No-op
    }

    @Override
    public void onCompletedTrade(String txHash) {
        Observable.just(txHash)
                .compose(RxUtil.applySchedulersToObservable())
                .subscribe(this::onTradeCompleted);
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "onReceiveValue: " + value);
    }

    @Override
    public void onShowTx(String txHash) {
        Log.d(TAG, "onShowTx: " + txHash);
    }

    private void applyFontToMenuItem(MenuItem menuItem) {
        if (typeface == null) {
            typeface = TypefaceUtils.load(getAssets(), "fonts/Montserrat-Regular.ttf");
        }
        menuItem.setTitle(CalligraphyUtils.applyTypefaceSpan(
                menuItem.getTitle(),
                typeface));
    }

    @Override
    public void clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();
        }
    }

    @Override
    public void onReceiveFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }

    @Override
    public void onTransactionNotesRequested(PaymentConfirmationDetails paymentConfirmationDetails,
                                            PaymentRequestType paymentRequestType,
                                            String contactId,
                                            long satoshis,
                                            int accountPosition) {
        addFragment(ContactConfirmRequestFragment.newInstance(paymentConfirmationDetails,
                paymentRequestType,
                contactId,
                satoshis,
                accountPosition));
    }

    @Override
    public void onRequestSuccessDismissed() {
        binding.bottomNavigation.setCurrentItem(1);
        getCurrentFragment().onResume();
    }

    @Override
    public void onChangeFeeClicked() {
        SendFragmentNew fragment = (SendFragmentNew) getSupportFragmentManager()
                .findFragmentByTag(SendFragmentNew.class.getSimpleName());
        fragment.onChangeFeeClicked();
    }

    @Override
    public void onSendClicked() {
        SendFragmentNew fragment = (SendFragmentNew) getSupportFragmentManager()
                .findFragmentByTag(SendFragmentNew.class.getSimpleName());
        fragment.onSendClicked();
    }

    @Override
    public void onPageFinished() {
        onStartBalanceFragment(false);
    }

    @Override
    public void onRequestSuccessful(@NotNull PaymentRequestType paymentRequestType,
                                    @NotNull String contactName,
                                    @NotNull String btcAmount) {
        addFragmentToBackStack(ContactRequestSuccessFragment.newInstance(paymentRequestType, contactName, btcAmount));
    }

    private void startSendFragment(@Nullable String scanData, @Nullable String scanRoute) {
        SendFragmentNew sendFragment = SendFragmentNew.Companion.newInstance(scanData, scanRoute, getSelectedAccountFromFragments());
        addFragmentToBackStack(sendFragment);
    }

    private void startReceiveFragment() {
        ReceiveFragment receiveFragment = ReceiveFragment.newInstance(getSelectedAccountFromFragments());
        addFragmentToBackStack(receiveFragment);
    }

    private void startDashboardFragment() {
        DashboardFragment fragment = DashboardFragment.newInstance();
        addFragmentToBackStack(fragment);
    }

    private int getSelectedAccountFromFragments() {
        if (getCurrentFragment() instanceof BalanceFragment) {
            return ((BalanceFragment) getCurrentFragment()).getSelectedAccountPosition();
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            return ((ReceiveFragment) getCurrentFragment()).getSelectedAccountPosition();
        } else {
            return -1;
        }
    }

    private void replaceFragmentWithAnimation(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName())
                .commitAllowingStateLoss();
    }

    private void addFragmentToBackStack(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .addToBackStack(fragment.getClass().getName())
                .add(R.id.content_frame, fragment, fragment.getClass().getSimpleName())
                .commitAllowingStateLoss();
    }

    private void addFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.content_frame, fragment, fragment.getClass().getSimpleName())
                .commitAllowingStateLoss();
    }

    private void handleIncomingIntent() {
        if (getIntent().hasExtra(EXTRA_URI)) {
            String uri = getIntent().getStringExtra(EXTRA_URI);
            String recipientId = getIntent().getStringExtra(EXTRA_RECIPIENT_ID);
            String mdid = getIntent().getStringExtra(EXTRA_MDID);
            String fctxId = getIntent().getStringExtra(EXTRA_FCTX_ID);

            startContactSendDialog(uri, recipientId, mdid, fctxId);
        }
    }

    private void startContactSendDialog(String uri, String recipientId, String mdid, String fctxId) {
        binding.bottomNavigation.removeOnTabSelectedListener();
        binding.bottomNavigation.setCurrentItem(0);
        binding.bottomNavigation.setOnTabSelectedListener(tabSelectedListener);
        addFragmentToBackStack(SendFragmentNew.Companion.newInstance(uri, recipientId, mdid, fctxId));
    }

    @Override
    public void showCustomPrompt(AppCompatDialogFragment alertFragments) {
        if (!isFinishing()) {
            alertFragments.show(getSupportFragmentManager(), alertFragments.getTag());
        }
    }

    @Override
    public Context getActivityContext() {
        return this;
    }

    @Override
    protected MainPresenter createPresenter() {
        return mainPresenter;
    }

    @Override
    protected MainView getView() {
        return this;
    }

    @Override
    public void showSecondPasswordDialog() {
        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setHint(R.string.password);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        FrameLayout frameLayout = ViewUtils.getAlertDialogPaddedView(this, editText);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.eth_now_supporting)
                .setMessage(R.string.eth_second_password_prompt)
                .setView(frameLayout)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    ViewUtils.hideKeyboard(this);
                    getPresenter().generateMetadataHDNodeAndEthereumWallet(editText.getText().toString());
                })
                .create()
                .show();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onSendFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }
}
