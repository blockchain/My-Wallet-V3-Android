package piuk.blockchain.android.ui.home;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import piuk.blockchain.android.R;
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
import piuk.blockchain.android.ui.send.SendActivity;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PermissionUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;

public class MainActivity extends BaseAuthActivity implements BalanceFragment.Communicator, MainViewModel.DataListener {

    private static final String SUPPORT_URI = "http://support.blockchain.com/";
    private static final int REQUEST_BACKUP = 2225;
    private static final int MERCHANT_ACTIVITY = 1;
    public static final int SCAN_URI = 2007;

    private boolean drawerIsOpen = false;

    private Toolbar toolbar;
    private MainViewModel mainViewModel;
    private ActivityMainBinding binding;
    private MaterialProgressDialog fetchTransactionsProgress;
    private AlertDialog mRootedDialog;

    private AppUtil appUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appUtil = new AppUtil(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainViewModel = new MainViewModel(this, this);
        binding.setViewModel(mainViewModel);

        mainViewModel.onViewReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUtil.deleteQR();

        mainViewModel.startWebSocketService();
        resetNavigationDrawer();
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

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                binding.drawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.action_qr:
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

    private Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.content_frame);
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
        }
    }

    @Override
    public void onBackPressed() {

        if (drawerIsOpen) {
            binding.drawerLayout.closeDrawers();

        } else if (getCurrentFragment() instanceof BalanceFragment) {
            if (((BalanceFragment) getCurrentFragment()).isFabExpanded()) {
                ((BalanceFragment) getCurrentFragment()).collapseFab();
            } else {
                mainViewModel.onBackPressed();
            }
        } else {
            Fragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
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
        Intent intent = new Intent(this, SendActivity.class);
        intent.putExtra("scan_data", strResult);
        startActivity(intent);
    }

    @Override
    public void setNavigationDrawerToggleEnabled(boolean enabled) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            toolbar.getChildAt(i).setEnabled(enabled);
            toolbar.getChildAt(i).setClickable(enabled);
        }

        if (enabled)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        else
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
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

        toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_menu_white_24dp));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        ViewUtils.setElevation(toolbar, 0F);

        View headerLayout = binding.nvView.getHeaderView(0);//TODO - future use for account selection
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
            drawable.setColorFilter(getResources().getColor(R.color.blockchain_send_red), PorterDuff.Mode.SRC_ATOP);
        } else {
            //Backed up
            drawable.setColorFilter(getResources().getColor(R.color.alert_green), PorterDuff.Mode.SRC_ATOP);
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
        if (!isFinishing()) {
            Handler handler = new Handler();
            handler.post(() -> {
                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commitAllowingStateLoss();
            });
        }
    }

    @Override
    public void onExitConfirmToast() {
        ToastCustom.makeText(this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    }
}
