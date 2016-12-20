package piuk.blockchain.android.ui.upgrade;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PasswordUtil;

import io.reactivex.exceptions.Exceptions;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.payload.PayloadBridge;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.databinding.ActivityUpgradeWalletBinding;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;

public class UpgradeWalletActivity extends BaseAuthActivity {

    private static final String TAG = "UpgradeWalletActivity";

    @Thunk PrefsUtil prefs;
    @Thunk AppUtil appUtil;
    @Thunk PayloadManager payloadManager;

    private ActivityUpgradeWalletBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade_wallet);

        payloadManager = PayloadManager.getInstance();
        prefs = new PrefsUtil(this);
        appUtil = new AppUtil(this);

        binding.upgradePageHeader.setFactory(() -> {
            TextView myText = new TextView(this);
            myText.setGravity(Gravity.CENTER);
            myText.setTextSize(14);
            myText.setTextColor(Color.WHITE);
            return myText;
        });
        binding.upgradePageHeader.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_fade_in));
        binding.upgradePageHeader.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_fade_out));
        binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_1));

        CustomPagerAdapter mCustomPagerAdapter = new CustomPagerAdapter(this);
        binding.pager.setAdapter(mCustomPagerAdapter);
        binding.pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setSelectedPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (payloadManager.getTempPassword() == null) {
            // Force re-login
            appUtil.clearCredentialsAndRestart();
            ToastCustom.makeText(this, getString(R.string.upgrade_fail_info), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        if (PasswordUtil.getInstance().ddpw(payloadManager.getTempPassword()) || PasswordUtil.getInstance().getStrength(payloadManager.getTempPassword().toString()) < 50) {

            LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            final LinearLayout pwLayout = (LinearLayout) inflater.inflate(R.layout.modal_change_password, null);

            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.weak_password)
                    .setCancelable(false)
                    .setView(pwLayout)
                    .setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                        String password1 = ((EditText) pwLayout.findViewById(R.id.pw1)).getText().toString();
                        String password2 = ((EditText) pwLayout.findViewById(R.id.pw2)).getText().toString();

                        if (password1.length() < 4 || password1.length() > 255 || password2.length() < 4 || password2.length() > 255) {
                            ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        } else {

                            if (!password2.equals(password1)) {
                                ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.password_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            } else {

                                final CharSequenceX currentPassword = payloadManager.getTempPassword();
                                payloadManager.setTempPassword(new CharSequenceX(password2));

                                AccessState.getInstance().createPin(payloadManager.getTempPassword(), AccessState.getInstance().getPIN())
                                        .subscribe(success -> {
                                            if (success) {
                                                PayloadBridge.getInstance().remoteSaveThread(null);
                                                ToastCustom.makeText(this, getString(R.string.password_changed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                            } else {
                                                throw Exceptions.propagate(new Throwable("Create PIN failed"));
                                            }
                                        }, throwable -> {
                                            payloadManager.setTempPassword(currentPassword);
                                            ToastCustom.makeText(this, getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                            ToastCustom.makeText(this, getString(R.string.password_unchanged), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                        });
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, (dialog, whichButton) -> ToastCustom.makeText(this, getString(R.string.password_unchanged), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL)).show();

        }
    }

    public void upgradeClicked(View view) {

        new SecondPasswordHandler(UpgradeWalletActivity.this).validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                doUpgrade(new CharSequenceX(""));
            }

            @Override
            public void onSecondPasswordValidated(String validateSecondPassword) {
                doUpgrade(new CharSequenceX(validateSecondPassword));
            }
        });
    }

    @Thunk
    void doUpgrade(final CharSequenceX secondPassword) {

        onUpgradeStart();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void[] params) {
                try {
                    if (ConnectivityStatus.hasConnectivity(UpgradeWalletActivity.this)) {
                        appUtil.setNewlyCreated(true);
                        appUtil.applyPRNGFixes();

                        payloadManager.upgradeV2PayloadToV3(
                                secondPassword,
                                appUtil.isNewlyCreated(),
                                getResources().getString(R.string.default_wallet_name),
                                new PayloadManager.UpgradePayloadListener() {
                                    @Override
                                    public void onDoubleEncryptionPasswordError() {
                                        ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.double_encryption_password_error),
                                                ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                        upgradeClicked(null);
                                    }

                                    @Override
                                    public void onUpgradeSuccess() {
                                        if (new OSUtil(UpgradeWalletActivity.this).isServiceRunning(WebSocketService.class)) {
                                            stopService(new Intent(UpgradeWalletActivity.this,
                                                    WebSocketService.class));
                                        }
                                        startService(new Intent(UpgradeWalletActivity.this,
                                                WebSocketService.class));

                                        payloadManager.getPayload().getHdWallet().getAccounts().get(0).setLabel(getResources().getString(R.string.default_wallet_name));
                                        onUpgradeCompleted();
                                    }

                                    @Override
                                    public void onUpgradeFail() {
                                        onUpgradeFailed();
                                    }
                                });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: ", e);
                }

                return null;
            }
        }.execute();
    }

    private void onUpgradeStart() {
        runOnUiThread(() -> {
            binding.upgradePageTitle.setText(getString(R.string.upgrading));
            binding.upgradePageHeader.setText(getString(R.string.upgrading_started_info));
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.pager.setVisibility(View.GONE);
            binding.upgradeActionContainer.setVisibility(View.GONE);
        });
    }

    @Thunk
    void onUpgradeCompleted() {

        runOnUiThread(() -> {
            binding.upgradePageTitle.setText(getString(R.string.upgrade_success_heading));
            binding.upgradePageHeader.setText(getString(R.string.upgrade_success_info));
            binding.progressBar.setVisibility(View.GONE);
            binding.btnUpgradeComplete.setVisibility(View.VISIBLE);
            binding.btnUpgradeComplete.setOnClickListener(v -> {
                prefs.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                AccessState.getInstance().setIsLoggedIn(true);
                appUtil.restartAppWithVerifiedPin();
            });
        });
    }

    @Thunk
    void onUpgradeFailed() {

        appUtil.setNewlyCreated(false);

        runOnUiThread(() -> {
            binding.upgradePageTitle.setText(getString(R.string.upgrade_fail_heading));
            binding.upgradePageHeader.setText(getString(R.string.upgrade_fail_info));
            binding.progressBar.setVisibility(View.GONE);
            binding.btnUpgradeComplete.setVisibility(View.VISIBLE);
            binding.btnUpgradeComplete.setText(getString(R.string.CLOSE));
            binding.btnUpgradeComplete.setOnClickListener(v -> onBackPressed());
        });
    }

    @Thunk
    void setSelectedPage(int position) {

        setBackGround(binding.pageBox0, R.drawable.rounded_view_dark_blue);
        setBackGround(binding.pageBox1, R.drawable.rounded_view_dark_blue);
        setBackGround(binding.pageBox2, R.drawable.rounded_view_dark_blue);

        switch (position) {
            case 0:
                binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_1));
                setBackGround(binding.pageBox0, R.drawable.rounded_view_upgrade_wallet_blue);
                break;
            case 1:
                binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_2));
                setBackGround(binding.pageBox1, R.drawable.rounded_view_upgrade_wallet_blue);
                break;
            case 2:
                binding.upgradePageHeader.setText(getResources().getString(R.string.upgrade_page_3));
                setBackGround(binding.pageBox2, R.drawable.rounded_view_upgrade_wallet_blue);
                break;
        }
    }

    private void setBackGround(View view, int res){
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            //noinspection deprecation
            view.setBackgroundDrawable(ContextCompat.getDrawable(this, res));
        } else {
            view.setBackground(ContextCompat.getDrawable(this, res));
        }
    }

    static class CustomPagerAdapter extends PagerAdapter {

        Context mContext;
        LayoutInflater mLayoutInflater;
        int[] mResources = {
                R.drawable.upgrade_backup_hilite,
                R.drawable.upgrade_hd_address_hilite,
                R.drawable.upgrade_tx_list_hilite,
        };

        CustomPagerAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.activity_upgrade_wallet_pager_item, container, false);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.imageView);
            imageView.setImageResource(mResources[position]);

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }

    }

    @Override
    public void onBackPressed() {
        new AccessState().logout(this);
        super.onBackPressed();
    }
}