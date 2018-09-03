package piuk.blockchain.android.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.blockchain.koin.modules.MorphActivityLauncher;
import com.blockchain.koin.modules.MorphMethodModuleKt;
import com.blockchain.morph.ui.homebrew.exchange.ExchangeActivity;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;
import com.karumi.dexter.listener.single.CompositePermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;
import info.blockchain.wallet.util.FormatsUtil;
import io.reactivex.Observable;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.databinding.ActivityMainBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.AccountActivity;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.backup.BackupWalletActivity;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.buy.BuyActivity;
import piuk.blockchain.android.ui.buy.FrontendJavascript;
import piuk.blockchain.android.ui.buy.FrontendJavascriptManager;
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherActivity;
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.contacts.payments.ContactConfirmRequestFragment;
import piuk.blockchain.android.ui.contacts.success.ContactRequestSuccessFragment;
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener;
import piuk.blockchain.android.ui.dashboard.DashboardFragment;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.ui.pairingcode.PairingCodeActivity;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.contacts.models.PaymentRequestType;
import info.blockchain.balance.CryptoCurrency;
import piuk.blockchain.androidcore.utils.annotations.Thunk;
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity;
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AndroidUtils;
import piuk.blockchain.androidcoreui.utils.AppUtil;
import piuk.blockchain.androidcoreui.utils.ViewUtils;
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont;
import piuk.blockchain.androidcoreui.utils.helperfunctions.FontHelpersKt;
import timber.log.Timber;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.EXTRA_METADATA_URI;

public class MainActivity extends BaseMvpActivity<MainView, MainPresenter> implements
        BalanceFragment.OnFragmentInteractionListener,
        MainView,
        SendFragment.OnSendFragmentInteractionListener,
        ReceiveFragment.OnReceiveFragmentInteractionListener,
        ContactConfirmRequestFragment.FragmentInteractionListener,
        FrontendJavascript<String>,
        ConfirmPaymentDialog.OnConfirmDialogInteractionListener,
        ContactRequestSuccessFragment.ContactsRequestSuccessListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String ACTION_SEND = "info.blockchain.wallet.ui.BalanceFragment.SEND";
    public static final String ACTION_RECEIVE = "info.blockchain.wallet.ui.BalanceFragment.RECEIVE";
    public static final String ACTION_RECEIVE_ETH = "info.blockchain.wallet.ui.BalanceFragment.RECEIVE_ETH";
    public static final String ACTION_RECEIVE_BCH = "info.blockchain.wallet.ui.BalanceFragment.RECEIVE_BCH";
    public static final String ACTION_BUY = "info.blockchain.wallet.ui.BalanceFragment.BUY";
    public static final String ACTION_SHAPESHIFT = "info.blockchain.wallet.ui.BalanceFragment.SHAPESHIFT";
    public static final String ACTION_BTC_BALANCE = "info.blockchain.wallet.ui.BalanceFragment.ACTION_BTC_BALANCE";
    public static final String ACTION_ETH_BALANCE = "info.blockchain.wallet.ui.BalanceFragment.ACTION_ETH_BALANCE";
    public static final String ACTION_BCH_BALANCE = "info.blockchain.wallet.ui.BalanceFragment.ACTION_BCH_BALANCE";

    private static final String SUPPORT_URI = "https://support.blockchain.com/";
    private static final int REQUEST_BACKUP = 2225;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    public static final String EXTRA_URI = "transaction_uri";
    public static final String EXTRA_RECIPIENT_ID = "recipient_id";
    public static final String EXTRA_MDID = "mdid";
    public static final String EXTRA_FCTX_ID = "fctx_id";

    public static final int SCAN_URI = 2007;
    public static final int ACCOUNT_EDIT = 2008;
    public static final int SETTINGS_EDIT = 2009;
    public static final int CONTACTS_EDIT = 2010;

    @Thunk
    boolean drawerIsOpen = false;
    private boolean handlingResult = false;

    @Inject
    MainPresenter mainPresenter;
    @Inject
    AppUtil appUtil;
    @Inject
    MorphActivityLauncher morphActivityLauncher;
    @Thunk
    ActivityMainBinding binding;
    private MaterialProgressDialog materialProgressDialog;
    private long backPressed;
    private Toolbar toolbar;
    @Thunk
    boolean paymentMade = false;
    private BalanceFragment balanceFragment;
    private FrontendJavascriptManager frontendJavascriptManager;
    private WebViewLoginDetails webViewLoginDetails;
    private boolean initialized;
    // Fragment callbacks for currency header
    private Map<View, OnTouchOutsideViewListener> touchOutsideViews = new HashMap<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(ACTION_SEND) && getActivity() != null) {
                requestScan();
            } else if (intent.getAction().equals(ACTION_RECEIVE) && getActivity() != null) {
                getPresenter().setCryptoCurrency(CryptoCurrency.BTC);
                binding.bottomNavigation.setCurrentItem(3);
            } else if (intent.getAction().equals(ACTION_RECEIVE_ETH) && getActivity() != null) {
                getPresenter().setCryptoCurrency(CryptoCurrency.ETHER);
                binding.bottomNavigation.setCurrentItem(3);
            } else if (intent.getAction().equals(ACTION_RECEIVE_BCH) && getActivity() != null) {
                getPresenter().setCryptoCurrency(CryptoCurrency.BCH);
                binding.bottomNavigation.setCurrentItem(3);
            } else if (intent.getAction().equals(ACTION_BUY) && getActivity() != null) {
                getPresenter().routeToBuySell();
            } else if (intent.getAction().equals(ACTION_SHAPESHIFT) && getActivity() != null) {
                MorphMethodModuleKt.launchAsync(morphActivityLauncher, MainActivity.this);
            } else if (intent.getAction().equals(ACTION_BTC_BALANCE)) {
                getPresenter().setCryptoCurrency(CryptoCurrency.BTC);
                // This forces the balance page to reload
                paymentMade = true;
                binding.bottomNavigation.setCurrentItem(2);
            } else if (intent.getAction().equals(ACTION_ETH_BALANCE)) {
                getPresenter().setCryptoCurrency(CryptoCurrency.ETHER);
                // This forces the balance page to reload
                paymentMade = true;
                binding.bottomNavigation.setCurrentItem(2);
            } else if (intent.getAction().equals(ACTION_BCH_BALANCE)) {
                getPresenter().setCryptoCurrency(CryptoCurrency.BCH);
                // This forces the balance page to reload
                paymentMade = true;
                binding.bottomNavigation.setCurrentItem(2);
            }
        }
    };

    private AHBottomNavigation.OnTabSelectedListener tabSelectedListener = (position, wasSelected) -> {

        getPresenter().doTestnetCheck();

        if (!wasSelected) {
            switch (position) {
                case 0:
                    if (!(getCurrentFragment() instanceof SendFragment)) {
                        // This is a bit of a hack to allow the selection of the correct button
                        // On the bottom nav bar, but without starting the fragment again
                        startSendFragment(null);
                        ViewUtils.setElevation(binding.appbarLayout, 0f);
                    }
                    break;
                case 1:
                    startDashboardFragment();
                    ViewUtils.setElevation(binding.appbarLayout, 4f);
                    break;
                case 2:
                    onStartBalanceFragment(paymentMade);
                    ViewUtils.setElevation(binding.appbarLayout, 0f);
                    break;
                case 3:
                    startReceiveFragment();
                    ViewUtils.setElevation(binding.appbarLayout, 0f);
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
        IntentFilter filterReceiveEth = new IntentFilter(ACTION_RECEIVE_ETH);
        IntentFilter filterReceiveBch = new IntentFilter(ACTION_RECEIVE_BCH);
        IntentFilter filterBuy = new IntentFilter(ACTION_BUY);
        IntentFilter filterShapeshift = new IntentFilter(ACTION_SHAPESHIFT);
        IntentFilter filterBtcBalance = new IntentFilter(ACTION_BTC_BALANCE);
        IntentFilter filterEthBalance = new IntentFilter(ACTION_ETH_BALANCE);
        IntentFilter filterBchBalance = new IntentFilter(ACTION_BCH_BALANCE);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterSend);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterReceive);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterBuy);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterReceiveEth);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterReceiveBch);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterShapeshift);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterBtcBalance);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterEthBalance);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterBchBalance);

        balanceFragment = BalanceFragment.newInstance(false);

        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // No-op
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                drawerIsOpen = true;
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
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
        FontHelpersKt.loadFont(this, CustomFont.MONTSERRAT_LIGHT, typeface -> {
            binding.bottomNavigation.setTitleTypeface(typeface);
            return Unit.INSTANCE;
        });

        // Select Dashboard by default
        binding.bottomNavigation.setOnTabSelectedListener(tabSelectedListener);
        binding.bottomNavigation.setCurrentItem(1);

        handleIncomingIntent();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        // This can null out in low memory situations, so reset here
        binding.navigationView.setNavigationItemSelectedListener(menuItem -> {
            selectDrawerItem(menuItem);
            return true;
        });
        appUtil.deleteQR();
        getPresenter().updateTicker();
        if (!handlingResult) {
            resetNavigationDrawer();
        }
        handlingResult = false;
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
        handlingResult = true;
        if (resultCode == RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            doScanInput(strResult);

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetNavigationDrawer();
        } else if (requestCode == SETTINGS_EDIT || requestCode == CONTACTS_EDIT || requestCode == ACCOUNT_EDIT) {
            // Re-init balance & dashboard fragment so that they reload all accounts/settings incase of changes
            if (balanceFragment != null) {
                balanceFragment = BalanceFragment.newInstance(false);
            }
            replaceFragment(DashboardFragment.newInstance());
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
            ((BalanceFragment) getCurrentFragment()).onBackPressed();
        } else if (getCurrentFragment() instanceof SendFragment) {
            ((SendFragment) getCurrentFragment()).onBackPressed();
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            ((ReceiveFragment) getCurrentFragment()).onBackPressed();
        } else //noinspection StatementWithEmptyBody
            if (getCurrentFragment() instanceof DashboardFragment) {
                handleBackPressed();
            } else if (getCurrentFragment() instanceof ContactConfirmRequestFragment) {
                // Remove Notes fragment from stack
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().remove(getCurrentFragment()).commit();
            } else {
                // Switch to balance fragment
                balanceFragment = BalanceFragment.newInstance(false);
                replaceFragment(balanceFragment);
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

    private void doScanInput(String strResult) {
        if (FormatsUtil.isValidBitcoinAddress(strResult)) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.confirm_currency)
                    .setMessage(R.string.confirm_currency_message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.bitcoin_cash, (dialog, which) -> {
                        getPresenter().setCryptoCurrency(CryptoCurrency.BCH);
                        startSendFragment(strResult);
                    })
                    .setNegativeButton(R.string.bitcoin, (dialog, which) -> {
                        getPresenter().setCryptoCurrency(CryptoCurrency.BTC);
                        startSendFragment(strResult);
                    })
                    .create()
                    .show();
        } else {
            startSendFragment(strResult);
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_backup:
                startActivityForResult(new Intent(this, BackupWalletActivity.class), REQUEST_BACKUP);
                break;
            case R.id.nav_exchange:
                MorphMethodModuleKt.launchAsync(morphActivityLauncher, MainActivity.this);
                break;
            case R.id.nav_exchange_homebrew_debug:
                startActivity(ExchangeActivity.Companion.intent(this, "GBP"));
                break;
            case R.id.nav_addresses:
                startActivityForResult(new Intent(this, AccountActivity.class), ACCOUNT_EDIT);
                break;
            case R.id.nav_buy:
                getPresenter().routeToBuySell();
                break;
            case R.id.nav_contacts:
                startActivityForResult(new Intent(this, ContactsListActivity.class), CONTACTS_EDIT);
                break;
            case R.id.login_web_wallet:
                PairingCodeActivity.start(this);
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

        // Set selected appropriately.
        if (getCurrentFragment() instanceof DashboardFragment) {
            binding.bottomNavigation.setCurrentItem(1);
        } else if (getCurrentFragment() instanceof BalanceFragment) {
            binding.bottomNavigation.setCurrentItem(2);
        } else if (getCurrentFragment() instanceof SendFragment) {
            binding.bottomNavigation.setCurrentItem(0);
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            binding.bottomNavigation.setCurrentItem(3);
        }

        if (!BuildConfig.CONTACTS_ENABLED) {
            MenuItem contactsMenuItem = getMenu().findItem(R.id.nav_contacts);
            contactsMenuItem.setVisible(false);
        }
    }

    @Thunk
    void requestScan() {
        SnackbarOnDeniedPermissionListener deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                .with(binding.getRoot(), R.string.request_camera_permission)
                .withButton(android.R.string.ok, v -> requestScan())
                .build();

        BasePermissionListener grantedPermissionListener = new BasePermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                startScanActivity();
            }
        };

        CompositePermissionListener compositePermissionListener =
                new CompositePermissionListener(deniedPermissionListener, grantedPermissionListener);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(compositePermissionListener)
                .withErrorListener(error -> Timber.wtf("Dexter permissions error " + error))
                .check();
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Thunk
    Context getActivity() {
        return this;
    }

    @Override
    public void showMetadataNodeFailure() {
        if (!isFinishing()) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.metadata_load_failure)
                    .setPositiveButton(R.string.retry, (dialog, which) -> getPresenter().initMetadataElements())
                    .setNegativeButton(R.string.exit, (dialog, which) -> AccessState.getInstance().logout(this))
                    .setCancelable(false)
                    .create()
                    .show();
        }
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
        if (!isFinishing()) {
            materialProgressDialog = new MaterialProgressDialog(this);
            materialProgressDialog.setCancelable(false);
            materialProgressDialog.setMessage(message);
            materialProgressDialog.show();
        }
    }

    @Override
    public void hideProgressDialog() {
        if (!isFinishing() && materialProgressDialog != null) {
            materialProgressDialog.dismiss();
            materialProgressDialog = null;
        }
    }

    @Override
    public void onScanInput(String strUri) {
        doScanInput(strUri);
    }

    @Override
    public void onStartBalanceFragment(boolean paymentToContactMade) {
        if (paymentToContactMade) {
            balanceFragment = BalanceFragment.newInstance(true);
            paymentMade = false;
        }
        replaceFragment(balanceFragment);
        toolbar.setTitle("");

        balanceFragment.refreshSelectedCurrency();
    }

    public AHBottomNavigation getBottomNavigationView() {
        return binding.bottomNavigation;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean isBuySellPermitted() {
        return AndroidUtils.is19orHigher();
    }

    @Override
    public void setBuySellEnabled(boolean enabled, boolean useWebView) {
        if (enabled && useWebView) {
            // For legacy SFOX + Unocoin only
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
                    // Add balance page to back stack
                    onStartBalanceFragment(false);
                    // Show transaction detail
                    Bundle bundle = new Bundle();
                    bundle.putString(BalanceFragment.KEY_TRANSACTION_HASH, txHash);
                    TransactionDetailActivity.start(this, bundle);
                }).show();
    }

    private void setBuyBitcoinVisible(boolean visible) {
        Menu menu = getMenu();
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
        buyWebView.loadUrl(getPresenter().getCurrentServerUrl());

        frontendJavascriptManager = new FrontendJavascriptManager(this, buyWebView);
        buyWebView.addJavascriptInterface(frontendJavascriptManager, FrontendJavascriptManager.JS_INTERFACE_NAME);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void checkTradesIfReady() {
        if (initialized && webViewLoginDetails != null && isBuySellPermitted()) {
            frontendJavascriptManager.checkForCompletedTrades(webViewLoginDetails);
        }
    }

    public void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails) {
        Timber.d("setWebViewLoginDetails: called");
        this.webViewLoginDetails = webViewLoginDetails;
        checkTradesIfReady();
    }

    @Override
    public void onFrontendInitialized() {
        Timber.d("onFrontendInitialized: called");
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
        Timber.d("onReceiveValue: %s", value);
    }

    @Override
    public void onShowTx(String txHash) {
        Timber.d("onShowTx: %s", txHash);
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
    public void onTransactionNotesRequested(@NonNull PaymentConfirmationDetails paymentConfirmationDetails,
                                            @NonNull PaymentRequestType paymentRequestType,
                                            @NonNull String contactId,
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
        SendFragment fragment = (SendFragment) getSupportFragmentManager()
                .findFragmentByTag(SendFragment.class.getSimpleName());
        fragment.onChangeFeeClicked();
    }

    @Override
    public void onSendClicked() {
        SendFragment fragment = (SendFragment) getSupportFragmentManager()
                .findFragmentByTag(SendFragment.class.getSimpleName());
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

    private void startSendFragment(@Nullable String scanData) {
        binding.bottomNavigation.removeOnTabSelectedListener();
        binding.bottomNavigation.setCurrentItem(0);
        ViewUtils.setElevation(binding.appbarLayout, 0f);
        binding.bottomNavigation.setOnTabSelectedListener(tabSelectedListener);
        SendFragment sendFragment =
                SendFragment.newInstance(scanData, getSelectedAccountFromFragments());
        addFragmentToBackStack(sendFragment);
    }

    private void startReceiveFragment() {
        ReceiveFragment receiveFragment =
                ReceiveFragment.newInstance(getSelectedAccountFromFragments());
        addFragmentToBackStack(receiveFragment);
    }

    private void startDashboardFragment() {
        DashboardFragment fragment = DashboardFragment.newInstance();
        addFragmentToBackStack(fragment);
    }

    public void showTestnetWarning() {
        if (getActivity() != null) {
            Snackbar snack = Snackbar.make(
                    binding.coordinatorLayout,
                    R.string.testnet_warning,
                    Snackbar.LENGTH_SHORT
            );
            View view = snack.getView();
            view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.product_red_medium));
            snack.show();
        }
    }

    @Override
    public void onStartLegacyBuySell() {
        BuyActivity.start(this);
    }

    @Override
    public void onStartBuySell() {
        BuySellLauncherActivity.start(this);
    }

    private int getSelectedAccountFromFragments() {
        if (getCurrentFragment() instanceof ReceiveFragment) {
            return ((ReceiveFragment) getCurrentFragment()).getSelectedAccountPosition();
        } else {
            return -1;
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName())
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
        addFragmentToBackStack(SendFragment.newInstance(uri, recipientId, mdid, fctxId));
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
                    getPresenter().decryptAndSetupMetadata(editText.getText().toString());
                })
                .create()
                .show();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showExchange() {
        getMenu().findItem(R.id.nav_exchange).setVisible(true);
    }

    @Override
    public void hideExchange() {
        getMenu().findItem(R.id.nav_exchange).setVisible(false);
    }

    @Override
    public void showHomebrewDebug() {
        getMenu().findItem(R.id.nav_exchange_homebrew_debug).setVisible(true);
    }

    private Menu getMenu() {
        return binding.navigationView.getMenu();
    }

    @Override
    public void onSendFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }

    public void setOnTouchOutsideViewListener(View view,
                                              OnTouchOutsideViewListener onTouchOutsideViewListener) {
        touchOutsideViews.put(view, onTouchOutsideViewListener);
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        // TODO: 16/02/2018 This is currently broken, revisit in the future
//        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
//            for (View view : touchOutsideViews.keySet()) {
//                // Notify touchOutsideViewListeners if user tapped outside a given view
//                Rect viewRect = new Rect();
//                view.getGlobalVisibleRect(viewRect);
//                if (!viewRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
//                    touchOutsideViews.get(view).onTouchOutside(view, ev);
//                }
//            }
//
//        }
        return super.dispatchTouchEvent(ev);
    }

}
