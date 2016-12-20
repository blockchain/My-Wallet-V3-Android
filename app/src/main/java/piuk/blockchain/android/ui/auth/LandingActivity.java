package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.api.DebugSettings;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.databinding.ActivityLandingBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.pairing.PairOrCreateWalletActivity;
import piuk.blockchain.android.util.AppUtil;


public class LandingActivity extends BaseAuthActivity {

    public static final String KEY_STARTING_FRAGMENT = "starting_fragment";
    public static final String KEY_INTENT_RECOVERING_FUNDS = "recovering_funds";

    private ActivityLandingBinding binding;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CREATE_FRAGMENT, LOGIN_FRAGMENT})
    @interface StartingFragment {
    }

    public static final int CREATE_FRAGMENT = 0;
    public static final int LOGIN_FRAGMENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_landing);
        setTitle(R.string.app_name);

        DebugSettings debugSettings = new DebugSettings();

        if (debugSettings.shouldShowDebugMenu()) {
            ToastCustom.makeText(
                    this,
                    "Current environment: "
                            + debugSettings.getCurrentEnvironment().getName(),
                    ToastCustom.LENGTH_SHORT,
                    ToastCustom.TYPE_GENERAL);

            binding.buttonSettings.setVisibility(View.VISIBLE);
            binding.buttonSettings.setOnClickListener(view ->
                    new EnvironmentSwitcher(this, debugSettings).showEnvironmentSelectionDialog());
        }

        binding.create.setOnClickListener(view -> startLandingActivity(CREATE_FRAGMENT));
        binding.login.setOnClickListener(view -> startLandingActivity(LOGIN_FRAGMENT));
        binding.recoverFunds.setOnClickListener(view -> showFundRecoveryWarning());

        if (!ConnectivityStatus.hasConnectivity(this)) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setMessage(getString(R.string.check_connectivity_exit))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue, (d, id) -> {
                        Intent intent = new Intent(LandingActivity.this, LandingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    })
                    .create()
                    .show();
        }
    }

    private void startLandingActivity(@StartingFragment int createFragment) {
        Intent intent = new Intent(this, PairOrCreateWalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_STARTING_FRAGMENT, createFragment);
        startActivity(intent);
    }

    private void startRecoveryActivityFlow() {
        Intent intent = new Intent(this, PairOrCreateWalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_STARTING_FRAGMENT, CREATE_FRAGMENT);
        intent.putExtra(KEY_INTENT_RECOVERING_FUNDS, true);
        startActivity(intent);
    }

    private void showFundRecoveryWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.recover_funds_warning_message)
                .setPositiveButton(R.string.dialog_continue, (dialogInterface, i) -> startRecoveryActivityFlow())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void startLogoutTimer() {
        // No-op
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user creates a new wallet or enters confidential information
        // consume event
        return new AppUtil(this).detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }
}
