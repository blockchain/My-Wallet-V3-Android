package info.blockchain.wallet;

import com.google.zxing.client.android.CaptureActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.drawer.DrawerAdapter;
import info.blockchain.wallet.drawer.DrawerItem;
import info.blockchain.wallet.listeners.RecyclerItemClickListener;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.EnableGeo;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.RootUtil;
import info.blockchain.wallet.util.SSLVerifierThreadUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.WebUtil;

import org.bitcoinj.core.bip44.WalletFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import piuk.blockchain.android.R;

public class MainActivity extends ActionBarActivity implements BalanceFragment.Communicator {

    private static final int SCAN_URI = 2007;
    private static final int REQUEST_BACKUP = 2225;
    public static boolean drawerIsOpen = false;
    public static Fragment currentFragment;
    private static int MERCHANT_ACTIVITY = 1;
    public long sendFragmentBitcoinAmountStorage = 0;
    RecyclerView recyclerViewDrawer;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    int exitClickCount = 0;
    int exitClickCooldown = 2;//seconds
    // toolbar
    private Toolbar toolbar = null;
    private ArrayList<DrawerItem> drawerItems;
    private int backupWalletDrawerIndex;
    private DrawerAdapter adapterDrawer;
    private AlertDialog rootedAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Log out if started with the logout intent
        if (getIntent().getAction() != null && AppUtil.LOGOUT_ACTION.equals(getIntent().getAction())) {
            finish();
            System.exit(0);
            return;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        if (RootUtil.getInstance().isDeviceRooted() &&
                !PrefsUtil.getInstance(this).getValue("disable_root_warning", false)
                && rootedAlertDialog == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(getString(R.string.device_rooted))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.dismiss();
                                }
                            });

            rootedAlertDialog = builder.create();
            rootedAlertDialog.show();
        }

        AppUtil.getInstance(MainActivity.this).applyPRNGFixes();

        if (!ConnectivityStatus.hasConnectivity(this)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            final String message = getString(R.string.check_connectivity_exit);

            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.dismiss();
                                    Class c = null;
                                    if (PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
                                        c = LandingActivity.class;
                                    } else {
                                        c = PinEntryActivity.class;
                                    }
                                    startSingleActivity(c);
                                }
                            });

            builder.create().show();
        } else {

            SSLVerifierThreadUtil.getInstance(MainActivity.this).validateSSLThread();

            exchangeRateThread();

            boolean isPinValidated = false;
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.containsKey("verified")) {
                isPinValidated = extras.getBoolean("verified");
            }

            String action = getIntent().getAction();
            String scheme = getIntent().getScheme();
            if (action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
                PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.KEY_SCHEME_URL, getIntent().getData().toString());
            }

            //
            // No GUID? Treat as new installation
            //
            if (PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
                PayloadFactory.getInstance().setTempPassword(new CharSequenceX(""));
                startSingleActivity(LandingActivity.class);
            }
            //
            // No PIN ID? Treat as installed app without confirmed PIN
            //
            else if (PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1) {
                startSingleActivity(PinEntryActivity.class);
            }
            //
            // Installed app, check sanity
            //
            else if (!AppUtil.getInstance(MainActivity.this).isSane()) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(MainActivity.this.getString(R.string.not_sane_error))
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AppUtil.getInstance(MainActivity.this).clearCredentialsAndRestart();
                                AppUtil.getInstance(MainActivity.this).restartApp();
                            }
                        }).show();

            }
            //
            // Legacy app has not been prompted for upgrade
            //
            else if (isPinValidated && !PayloadFactory.getInstance().get().isUpgraded() && PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L) == 0L) {

                AccessFactory.getInstance(MainActivity.this).setIsLoggedIn(true);
                Intent intent = new Intent(MainActivity.this, UpgradeWalletActivity.class);
                startActivity(intent);

            }
            //
            // App has been PIN validated
            //
            else if (isPinValidated || (AccessFactory.getInstance(MainActivity.this).isLoggedIn())) {
                AccessFactory.getInstance(MainActivity.this).setIsLoggedIn(true);

                this.setSessionId();

                final ProgressDialog progress = new ProgressDialog(this);
                progress.setCancelable(false);
                progress.setTitle(R.string.app_name);
                progress.setMessage(getString(R.string.please_wait));
                progress.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Looper.prepare();

                        try {
                            HDPayloadBridge.getInstance(getApplicationContext()).updateBalancesAndTransactions();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }

                        if (PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_SCHEME_URL, "").length() > 0) {
                            String strUri = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_SCHEME_URL, "");
                            PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.KEY_SCHEME_URL, "");
                            doScanInput(strUri);
                        } else {

                            if(!isFinishing())    {
                                Handler handler = new Handler();
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Fragment fragment = new BalanceFragment();
                                        FragmentManager fragmentManager = getFragmentManager();
                                        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commitAllowingStateLoss();
                                    }
                                });
                            }

                        }

                        Looper.loop();
                    }
                }).start();
            } else {
                startSingleActivity(PinEntryActivity.class);
            }
        }

        setContentView(R.layout.activity_main);

        if (!getResources().getBoolean(R.bool.isRotatable))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setToolbar();
        setNavigationDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(MainActivity.this).stopLogoutTimer();
        AppUtil.getInstance(MainActivity.this).deleteQR();

        if (!OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
            startService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));
        }

        sendFragmentBitcoinAmountStorage = 0;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (rootedAlertDialog != null) {
            rootedAlertDialog.dismiss();
            rootedAlertDialog = null;
        }

        if (!OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
            stopService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));
        }

        AppUtil.getInstance(MainActivity.this).deleteQR();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_merchant_directory:
                doMerchantDirectory();
                return true;
            case R.id.action_qr:
                if (!AppUtil.getInstance(MainActivity.this).isCameraOpen()) {
                    scanURI();
                } else {
                    ToastCustom.makeText(MainActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        ;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            doScanInput(strResult);
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
            drawerItems = new ArrayList<>();
            final String[] drawerTitles = getResources().getStringArray(R.array.navigation_drawer_items_hd);
            final TypedArray drawerIcons = getResources().obtainTypedArray(R.array.navigation_drawer_icons_hd);
            for (int i = 0; i < drawerTitles.length; i++) {

                if (drawerTitles[i].equals(getResources().getString(R.string.backup_wallet))) {
                    backupWalletDrawerIndex = i;

                    if (!PayloadFactory.getInstance().get().getHdWallet().isMnemonicVerified()) {
                        //Not backed up0
                        drawerItems.add(new DrawerItem(drawerTitles[i], drawerIcons.getDrawable(i)));
                    } else {
                        //Backed up
                        drawerItems.add(new DrawerItem(drawerTitles[i], getResources().getDrawable(R.drawable.good_backup)));
                    }
                } else if (drawerTitles[i].equals(getResources().getString(R.string.upgrade_wallet)) && (PayloadFactory.getInstance().get().isUpgraded())) {
                    continue;//Wallet has been upgraded
                } else {
                    drawerItems.add(new DrawerItem(drawerTitles[i], drawerIcons.getDrawable(i)));
                }
            }
            drawerIcons.recycle();
            adapterDrawer = new DrawerAdapter(drawerItems);
            recyclerViewDrawer.setAdapter(adapterDrawer);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerIsOpen) {
            mDrawerLayout.closeDrawers();
        } else if (currentFragment instanceof BalanceFragment) {

            exitClickCount++;
            if (exitClickCount == 2) {
                if (OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
                    stopService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));
                }

                AppUtil.getInstance(this).logout();
            } else {
                ToastCustom.makeText(MainActivity.this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j <= exitClickCooldown; j++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (j >= exitClickCooldown) exitClickCount = 0;
                    }
                }
            }).start();

        } else if (currentFragment instanceof ReceiveFragment && ((ReceiveFragment) currentFragment).isKeypadVisible) {
            ((ReceiveFragment) currentFragment).onKeypadClose();

        } else if (currentFragment instanceof SendFragment && ((SendFragment) currentFragment).isKeypadVisible) {
            ((SendFragment) currentFragment).onKeypadClose();

        } else {
            Fragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    private void exchangeRateThread() {

        List<String> currencies = Arrays.asList(ExchangeRateFactory.getInstance(MainActivity.this).getCurrencies());
        String strCurrentSelectedFiat = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_SELECTED_FIAT, "");
        if (!currencies.contains(strCurrentSelectedFiat)) {
            PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        }

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                String response = null;
                try {
                    response = WebUtil.getInstance().getURL(WebUtil.EXCHANGE_URL);

                    ExchangeRateFactory.getInstance(MainActivity.this).setData(response);
                    ExchangeRateFactory.getInstance(MainActivity.this).updateFxPricesForEnabledCurrencies();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private void doSettings() {
        Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.SettingsActivity.class);
        startActivity(intent);
    }

    private void doExchangeRates() {
        if (hasZeroBlock()) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.phlint.android.zeroblock");
            startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.phlint.android.zeroblock"));
            startActivity(intent);
        }
    }

    private boolean hasZeroBlock() {
        PackageManager pm = this.getPackageManager();
        try {
            pm.getPackageInfo("com.phlint.android.zeroblock", 0);
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }

    private void scanURI() {
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, SCAN_URI);
    }

    private void doScanInput(String address) {

        String btc_address = null;
        String btc_amount = null;

        // check for poorly formed BIP21 URIs
        if (address.startsWith("bitcoin://") && address.length() > 10) {
            address = "bitcoin:" + address.substring(10);
        }

        if (FormatsUtil.getInstance().isValidBitcoinAddress(address)) {
            btc_address = address;
        } else if (FormatsUtil.getInstance().isBitcoinUri(address)) {
            btc_address = FormatsUtil.getInstance().getBitcoinAddress(address);
            btc_amount = FormatsUtil.getInstance().getBitcoinAmount(address);
        } else {
            ToastCustom.makeText(MainActivity.this, getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            AppUtil.getInstance(MainActivity.this).restartApp();
            return;
        }

        Fragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString("btc_address", btc_address);
        args.putBoolean("incoming_from_scan", true);
        if (btc_amount != null) {
            try {
                args.putString("btc_amount", MonetaryUtil.getInstance(this).getDisplayAmount(Long.parseLong(btc_amount)));
            } catch (NumberFormatException nfe) {
                ;
            }
        } else if (sendFragmentBitcoinAmountStorage > 0) {
            args.putString("btc_amount", MonetaryUtil.getInstance(this).getDisplayAmount(sendFragmentBitcoinAmountStorage));
        }
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

    }

    public void setToolbar() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_menu_white_24dp));
        setSupportActionBar(toolbar);
    }

    public void resetNavigationDrawer() {

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                drawerIsOpen = false;

                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    toolbar.getChildAt(i).setEnabled(true);
                    toolbar.getChildAt(i).setClickable(true);
                }
            }

            public void onDrawerOpened(View drawerView) {
                drawerIsOpen = true;

                InputMethodManager inputManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    toolbar.getChildAt(i).setEnabled(false);
                    toolbar.getChildAt(i).setClickable(false);
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
    }

    public void setNavigationDrawer() {

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Setup Drawer Icon
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                drawerIsOpen = false;

                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    toolbar.getChildAt(i).setEnabled(true);
                    toolbar.getChildAt(i).setClickable(true);
                }

            }

            public void onDrawerOpened(View drawerView) {
                drawerIsOpen = true;

                InputMethodManager inputManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    toolbar.getChildAt(i).setEnabled(false);
                    toolbar.getChildAt(i).setClickable(false);
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();

        // statusBar color behind navigation drawer
        TypedValue typedValueStatusBarColor = new TypedValue();
        MainActivity.this.getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValueStatusBarColor, true);
        final int colorStatusBar = typedValueStatusBarColor.data;
        mDrawerLayout.setStatusBarBackgroundColor(colorStatusBar);

        TextView tvEmail = (TextView) mDrawerLayout.findViewById(R.id.drawer_email);
        tvEmail.setText(PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_EMAIL, ""));

        // Setup RecyclerView inside drawer
        recyclerViewDrawer = (RecyclerView) findViewById(R.id.drawer_recycler);
        recyclerViewDrawer.setHasFixedSize(true);
        recyclerViewDrawer.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        drawerItems = new ArrayList<>();
        String[] drawerTitles = getResources().getStringArray(R.array.navigation_drawer_items_hd);
        TypedArray drawerIcons = getResources().obtainTypedArray(R.array.navigation_drawer_icons_hd);

        if (AppUtil.getInstance(this).isNotUpgraded()) {
            drawerTitles = getResources().getStringArray(R.array.navigation_drawer_items_lame);
            drawerIcons = getResources().obtainTypedArray(R.array.navigation_drawer_icons_lame);
        }

        for (int i = 0; i < drawerTitles.length; i++) {

            if (drawerTitles[i].equals(getResources().getString(R.string.backup_wallet))) {

                backupWalletDrawerIndex = i;

                if (!PayloadFactory.getInstance().get().getHdWallet().isMnemonicVerified()) {
                    //Not backed up
                    drawerItems.add(new DrawerItem(drawerTitles[i], drawerIcons.getDrawable(i)));
                } else {
                    //Backed up
                    drawerItems.add(new DrawerItem(drawerTitles[i], getResources().getDrawable(R.drawable.good_backup)));
                }

                continue;
            } else if (drawerTitles[i].equals(getResources().getString(R.string.backup_wallet))) {
                continue;//No backup for legacy wallets
            } else if (drawerTitles[i].equals(getResources().getString(R.string.upgrade_wallet)) && (PayloadFactory.getInstance().get().isUpgraded())) {
                continue;//Wallet has been upgraded
            }

            drawerItems.add(new DrawerItem(drawerTitles[i], drawerIcons.getDrawable(i)));
        }
        drawerIcons.recycle();
        adapterDrawer = new DrawerAdapter(drawerItems);
        recyclerViewDrawer.setAdapter(adapterDrawer);

        recyclerViewDrawer.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        if (AppUtil.getInstance(MainActivity.this).isNotUpgraded()) {
                            selectDrawerItemLegacy(position);
                        } else {
                            selectDrawerItemHd(position);
                        }

                        mDrawerLayout.closeDrawers();

                    }
                })
        );
    }

    private void selectDrawerItemLegacy(int position) {
        switch (position) {
            case 0:
                doUpgrade();
                break;
            case 1:
                doSettings();
                break;
            case 2:
                doChangePin();
                break;
            case 3:
                doMyAccounts();
                break;
            case 4:
                doExchangeRates();
                break;
            case 5:
                doSupport();
                break;
            case 6:
                doUnpairWallet();
                break;
        }
    }

    private void selectDrawerItemHd(int position) {
        switch (position) {
            case 0:
                doBackupWallet();
                break;
            case 1:
                doSettings();
                break;
            case 2:
                doChangePin();
                break;
            case 3:
                doMyAccounts();
                break;
            case 4:
                doExchangeRates();
                break;
            case 5:
                doSupport();
                break;
            case 6:
                doUnpairWallet();
                break;
        }
    }

    @Override
    public void setNavigationDrawerToggleEnabled(boolean enabled) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            toolbar.getChildAt(i).setEnabled(enabled);
            toolbar.getChildAt(i).setClickable(enabled);
        }

        if (enabled)
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        else
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void doChangePin() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.alert_change_pin, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
            }
        });

        TextView confirmChangePin = (TextView) dialogView.findViewById(R.id.confirm_unpair);
        confirmChangePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText pass = (EditText) dialogView.findViewById(R.id.password_confirm);

                if (pass.getText().toString().equals(PayloadFactory.getInstance().getTempPassword().toString())) {

                    PrefsUtil.getInstance(MainActivity.this).removeValue(PrefsUtil.KEY_PIN_FAILS);
                    PrefsUtil.getInstance(MainActivity.this).removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                    startSingleActivity(PinEntryActivity.class);
                    finish();

                    alertDialog.dismiss();
                } else {
                    pass.setText("");
                    ToastCustom.makeText(MainActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
            }
        });

        alertDialog.show();
    }

    private void doUnpairWallet() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_unpair_wallet, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
            }
        });

        TextView confirmUnpair = (TextView) dialogView.findViewById(R.id.confirm_unpair);
        confirmUnpair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
                    stopService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));
                }

                WalletFactory.getInstance().set(null);
                PayloadFactory.getInstance().wipe();
                MultiAddrFactory.getInstance().wipe();
                PrefsUtil.getInstance(MainActivity.this).clear();

                startService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));

                AppUtil.getInstance(MainActivity.this).restartApp();

                alertDialog.dismiss();
            }
        });

        alertDialog.show();

    }

    private void doMerchantDirectory() {

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            EnableGeo.displayGPSPrompt(this);
        } else {
            Intent intent = new Intent(MainActivity.this, info.blockchain.merchant.directory.MapActivity.class);
            startActivityForResult(intent, MERCHANT_ACTIVITY);
        }
    }

    private void doMyAccounts() {
        Intent intent = new Intent(MainActivity.this, AccountActivity.class);
        startActivity(intent);
    }

    private void doSupport() {
        Intent intent = new Intent(MainActivity.this, SupportActivity.class);
        startActivity(intent);
    }

    private void doBackupWallet() {
        Intent intent = new Intent(MainActivity.this, BackupWalletActivity.class);
        startActivityForResult(intent, REQUEST_BACKUP);
    }

    private void doUpgrade() {
        Intent intent = new Intent(MainActivity.this, UpgradeWalletActivity.class);
        startActivity(intent);
    }

    private void setSessionId() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                String sid = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_SESSION_ID, null);
                if (sid.isEmpty()) sid = null;

                if (sid == null) {
                    //Get new SID
                    String guid = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "");
                    try {
                        sid = WebUtil.getInstance().getCookie(WebUtil.PAYLOAD_URL + "/" + guid + "?format=json&resend_code=false", "SID");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (sid != null && !sid.isEmpty())
                        PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.KEY_SESSION_ID, sid);
                }

                Looper.loop();

            }
        }).start();
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
