package piuk.blockchain.android.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;

import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.databinding.ActivityMainBinding;
import piuk.blockchain.android.ui.account.AccountActivity;
import piuk.blockchain.android.ui.auth.LandingActivity;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.backup.BackupWalletActivity;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.settings.SettingsFragment.EXTRA_SHOW_ADD_EMAIL_DIALOG;

public class MainActivity extends BaseAuthActivity implements BalanceFragment.Communicator,
        MainViewModel.DataListener,
        SendFragment.OnSendFragmentInteractionListener,
        ReceiveFragment.OnReceiveFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SUPPORT_URI = "http://support.blockchain.com/";
    private static final int REQUEST_BACKUP = 2225;
    private static final int MERCHANT_ACTIVITY = 1;
    public static final int SCAN_URI = 2007;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    @Thunk boolean drawerIsOpen = false;

    private MainViewModel mainViewModel;
    private ActivityMainBinding binding;
    private MaterialProgressDialog fetchTransactionsProgress;
    private AlertDialog mRootedDialog;
    private AppUtil appUtil;
    private long backPressed;
    private boolean returningResult = false;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appUtil = new AppUtil(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainViewModel = new MainViewModel(this, this);

        mainViewModel.onViewReady();

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

        // Create items
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.send_bitcoin, R.drawable.vector_send, R.color.blockchain_pearl_white);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.transactions, R.drawable.vector_transactions, R.color.blockchain_pearl_white);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(R.string.receive_bitcoin, R.drawable.vector_receive, R.color.blockchain_pearl_white);

        // Add items
        binding.bottomNavigation.addItem(item1);
        binding.bottomNavigation.addItem(item2);
        binding.bottomNavigation.addItem(item3);

        binding.bottomNavigation.setAccentColor(ContextCompat.getColor(this, R.color.blockchain_blue));
        binding.bottomNavigation.setInactiveColor(ContextCompat.getColor(this, R.color.blockchain_grey));
        binding.bottomNavigation.setForceTint(true);
        binding.bottomNavigation.setUseElevation(true);

        // Select transactions by default
        binding.bottomNavigation.setCurrentItem(1);
        binding.bottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
            if (!wasSelected) {
                switch (position) {
                    case 0:
                        if (!(getCurrentFragment() instanceof SendFragment)) {
                            // This is a bit of a hack to allow the selection of the correct button
                            // On the bottom nav bar, but without starting the fragment again
                            startSendFragment(null);
                        }
                        break;
                    case 1:
                        onStartBalanceFragment();
                        break;
                    case 2:
                        startReceiveFragment();
                        break;
                }
            } else {
                if (position == 1 && getCurrentFragment() instanceof BalanceFragment) {
                    ((BalanceFragment) getCurrentFragment()).onScrollToTop();
                }
            }

            return true;
        });
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        appUtil.deleteQR();
        mainViewModel.storeSwipeReceiveAddresses();
        resetNavigationDrawer();

        if (AndroidUtils.is25orHigher() && mainViewModel.areLauncherShortcutsEnabled()) {
            LauncherShortcutHelper launcherShortcutHelper = new LauncherShortcutHelper(
                    this,
                    PayloadManager.getInstance(),
                    getSystemService(ShortcutManager.class));

            launcherShortcutHelper.generateReceiveShortcuts();
        }

        binding.bottomNavigation.restoreBottomNavigation(false);
        // Reset state of the bottom nav bar, but not if returning from a scan
        if (!returningResult) {
            runOnUiThread(() -> binding.bottomNavigation.setCurrentItem(1));
        }
        returningResult = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainViewModel.destroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                binding.drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_qr_main:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
                } else {
                    startScanActivity();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

            doScanInput(strResult);

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetNavigationDrawer();
        } else {
            if (data != null) {
                returningResult = true;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerIsOpen) {
            binding.drawerLayout.closeDrawers();
        } else if (getCurrentFragment() instanceof BalanceFragment) {
            handleBackPressed();
        } else if (getCurrentFragment() instanceof SendFragment) {
            ((SendFragment) getCurrentFragment()).onBackPressed();
        } else if (getCurrentFragment() instanceof ReceiveFragment) {
            ((ReceiveFragment) getCurrentFragment()).onBackPressed();
        } else {
            // Switch to balance fragment
            BalanceFragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
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

    private void startScanActivity() {
        if (!appUtil.isCameraOpen()) {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_URI);
        } else {
            ToastCustom.makeText(MainActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void doScanInput(String strResult) {
        startSendFragment(strResult);
    }

    public void selectDrawerItem(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_backup:
                startActivityForResult(new Intent(MainActivity.this, BackupWalletActivity.class), REQUEST_BACKUP);
                break;
            case R.id.nav_addresses:
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                break;
            case R.id.nav_upgrade:
                startActivity(new Intent(MainActivity.this, UpgradeWalletActivity.class));
                break;
            case R.id.nav_map:
                startMerchantActivity();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_support:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URI)));
                break;
            case R.id.nav_logout:
                new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                        .setTitle(R.string.unpair_wallet)
                        .setMessage(R.string.ask_you_sure_unpair)
                        .setPositiveButton(R.string.unpair, (dialog, which) -> mainViewModel.unpair())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
        }
        binding.drawerLayout.closeDrawers();
    }

    @Override
    public void resetNavigationDrawer() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_menu_white_24dp));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        ViewUtils.setElevation(toolbar, 0F);

        MenuItem backUpMenuItem = binding.nvView.getMenu().findItem(R.id.nav_backup);
        MenuItem upgradeMenuItem = binding.nvView.getMenu().findItem(R.id.nav_upgrade);

        if (mainViewModel.getPayloadManager().isNotUpgraded()) {
            //Legacy
            upgradeMenuItem.setVisible(true);
            backUpMenuItem.setVisible(false);
        } else {
            //HD
            upgradeMenuItem.setVisible(false);
            backUpMenuItem.setVisible(true);
        }

        MenuItem backUpView = binding.nvView.getMenu().findItem(R.id.nav_backup);
        Drawable drawable = backUpView.getIcon();
        drawable.mutate();
        if (mainViewModel.getPayloadManager().getPayload() != null &&
                mainViewModel.getPayloadManager().getPayload().getHdWallet() != null &&
                !mainViewModel.getPayloadManager().getPayload().getHdWallet().isMnemonicVerified()) {
            //Not backed up
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.blockchain_send_red), PorterDuff.Mode.SRC_ATOP);
        } else {
            //Backed up
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.alert_green), PorterDuff.Mode.SRC_ATOP);
        }

        binding.nvView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });
    }

    private void startMerchantActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestLocationPermissionFromActivity(binding.getRoot(), this);
        } else {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!enabled) {
                EnableGeo.displayGPSPrompt(this);
            } else {
                Intent intent = new Intent(MainActivity.this, piuk.blockchain.android.ui.directory.MapActivity.class);
                startActivityForResult(intent, MERCHANT_ACTIVITY);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRootedDialog != null && mRootedDialog.isShowing()) {
            mRootedDialog.dismiss();
        }
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

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

    @Override
    public void onRooted() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                mRootedDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setMessage(getString(R.string.device_rooted))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_continue, null)
                        .create();

                mRootedDialog.show();
            }
        }, 500);
    }

    private Context getActivity() {
        return this;
    }

    @Override
    public void showEmailVerificationDialog(String email) {
        String message = String.format(getString(R.string.security_centre_email_message), email);
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_email_title,
                message,
                R.drawable.vector_email,
                android.R.string.ok);
        securityPromptDialog.showDialog(getSupportFragmentManager());
        securityPromptDialog.setPositiveButtonListener(v -> securityPromptDialog.dismiss());
    }

    @Override
    public void showAddEmailDialog() {
        String message = getString(R.string.security_centre_add_email_message);
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_add_email_title,
                message,
                R.drawable.vector_email,
                R.string.security_centre_add_email_positive_button,
                true,
                false);
        securityPromptDialog.showDialog(getSupportFragmentManager());
        securityPromptDialog.setPositiveButtonListener(v -> {
            securityPromptDialog.dismiss();
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(EXTRA_SHOW_ADD_EMAIL_DIALOG, true);
            startActivity(intent);
        });
        securityPromptDialog.setNegativeButtonListener(view -> securityPromptDialog.dismiss());
    }

    @Override
    public void onConnectivityFail() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        final String message = getString(R.string.check_connectivity_exit);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue,
                        (d, id) -> {
                            d.dismiss();
                            Class c = null;
                            if (new PrefsUtil(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
                                c = LandingActivity.class;
                            } else {
                                c = PinEntryActivity.class;
                            }
                            startSingleActivity(c);
                        });

        builder.create().show();
    }

    @Override
    public void kickToLauncherPage() {
        startSingleActivity(LauncherActivity.class);
    }

    @Override
    public void onFetchTransactionsStart() {
        fetchTransactionsProgress = new MaterialProgressDialog(this);
        fetchTransactionsProgress.setCancelable(false);
        fetchTransactionsProgress.setMessage(getString(R.string.please_wait));
        fetchTransactionsProgress.show();
    }

    @Override
    public void onFetchTransactionCompleted() {
        if (fetchTransactionsProgress != null && fetchTransactionsProgress.isShowing()) {
            fetchTransactionsProgress.dismiss();
        }
    }

    @Override
    public void onScanInput(String strUri) {
        doScanInput(strUri);
    }

    @Override
    public void onStartBalanceFragment() {
        BalanceFragment fragment = new BalanceFragment();
        startFragmentWithAnimation(fragment);
    }

    public void startSendFragment(String scanData) {
        boolean isBTC;
        int selectedAccountPosition;
        try {
            isBTC = ((BalanceFragment) getCurrentFragment()).getIsBTC();
            selectedAccountPosition = ((BalanceFragment) getCurrentFragment()).getSelectedAccountPosition();
        } catch (ClassCastException e) {
            Log.e(TAG, "startSendFragment: ", e);
            isBTC = true;
            selectedAccountPosition = -1;
        }

        SendFragment sendFragment = SendFragment.newInstance(scanData, isBTC, selectedAccountPosition);
        startFragmentWithAnimation(sendFragment);
    }

    public void startReceiveFragment() {
        boolean isBTC;
        int selectedAccountPosition;
        try {
            isBTC = ((BalanceFragment) getCurrentFragment()).getIsBTC();
            selectedAccountPosition = ((BalanceFragment) getCurrentFragment()).getSelectedAccountPosition();
        } catch (ClassCastException e) {
            Log.e(TAG, "startReceiveFragment: ", e);
            isBTC = true;
            selectedAccountPosition = -1;
        }

        ReceiveFragment receiveFragment = ReceiveFragment.newInstance(isBTC, selectedAccountPosition);
        startFragmentWithAnimation(receiveFragment);
    }

    private void startFragmentWithAnimation(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.content_frame, fragment).commitAllowingStateLoss();
    }

    public AHBottomNavigation getBottomNavigationView() {
        return binding.bottomNavigation;
    }

    @Override
    public void clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();
        }
    }

    @Override
    public void onSendFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }

    // Ensure bottom nav button selected after scanning for result
    @Override
    public void onSendFragmentStart() {
        binding.bottomNavigation.setCurrentItem(0);
    }

    @Override
    public void onReceiveFragmentClose() {
        binding.bottomNavigation.setCurrentItem(1);
    }
}
