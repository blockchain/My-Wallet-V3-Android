package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PasswordUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;

import piuk.blockchain.android.R;

public class UpgradeWalletActivity extends Activity {

    private AlertDialog alertDialog = null;
    private ViewPager mViewPager = null;
    private CustomPagerAdapter mCustomPagerAdapter = null;

    private TextSwitcher pageHeader = null;

    private TextView pageBox0 = null;
    private TextView pageBox1 = null;
    private TextView pageBox2 = null;

    private TextView heading = null;
    private TextView info = null;
    private ProgressBar progressBar = null;
    private TextView confirmCancel = null;
    private TextView confirmUpgrade = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upgrade_wallet);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        pageHeader = (TextSwitcher) findViewById(R.id.upgrade_page_header);
        pageHeader.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                TextView myText = new TextView(UpgradeWalletActivity.this);
                myText.setGravity(Gravity.CENTER);
                myText.setTextSize(14);
                myText.setTextColor(Color.WHITE);
                return myText;
            }
        });
        pageHeader.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_fade_in));
        pageHeader.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_fade_out));
        pageHeader.setText(getResources().getString(R.string.upgrade_page_2));

        mCustomPagerAdapter = new CustomPagerAdapter(this);
        mViewPager.setAdapter(mCustomPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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

        if (PasswordUtil.getInstance().ddpw(PayloadFactory.getInstance().getTempPassword()) || PasswordUtil.getInstance().getStrength(PayloadFactory.getInstance().getTempPassword().toString()) < 50) {

            LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            final LinearLayout pwLayout = (LinearLayout) inflater.inflate(R.layout.modal_change_password, null);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.weak_password)
                    .setCancelable(false)
                    .setView(pwLayout)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            String password1 = ((EditText) pwLayout.findViewById(R.id.pw1)).getText().toString();
                            String password2 = ((EditText) pwLayout.findViewById(R.id.pw2)).getText().toString();

                            if (password1 == null || password1.length() < 9 || password1.length() > 255 ||
                                    password2 == null || password2.length() < 9 || password2.length() > 255) {
                                ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            } else {

                                if (!password2.equals(password1)) {
                                    ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.password_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } else {

                                    final CharSequenceX currentPassword = PayloadFactory.getInstance().getTempPassword();
                                    PayloadFactory.getInstance().setTempPassword(new CharSequenceX(password2));

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            Looper.prepare();

                                            if (AccessFactory.getInstance(UpgradeWalletActivity.this).createPIN(PayloadFactory.getInstance().getTempPassword(), AccessFactory.getInstance(UpgradeWalletActivity.this).getPIN())) {
                                                PayloadBridge.getInstance(UpgradeWalletActivity.this).remoteSaveThread();
                                                ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.password_changed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                            } else {
                                                PayloadFactory.getInstance().setTempPassword(currentPassword);
                                                ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                                ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.password_unchanged), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                            }

                                            Looper.loop();

                                        }
                                    }).start();

                                }

                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ToastCustom.makeText(UpgradeWalletActivity.this, getString(R.string.password_unchanged), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                        }
                    }).show();

        }

        pageBox0 = (TextView) findViewById(R.id.pageBox0);
        pageBox1 = (TextView) findViewById(R.id.pageBox1);
        pageBox2 = (TextView) findViewById(R.id.pageBox2);
    }

    public void upgradeClicked(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_upgrade_wallet_confirm, null);
        dialogBuilder.setView(dialogView);

        alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);

        heading = (TextView) dialogView.findViewById(R.id.heading_tv);
        info = (TextView) dialogView.findViewById(R.id.upgrade_info_tv);
        progressBar = (ProgressBar) dialogView.findViewById(R.id.progressBar3);

        confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();

                AccessFactory.getInstance(UpgradeWalletActivity.this).setIsLoggedIn(true);
                AppUtil.getInstance(UpgradeWalletActivity.this).restartApp("verified", true);
            }
        });

        confirmUpgrade = (TextView) dialogView.findViewById(R.id.confirm_upgrade);
        confirmUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                    final EditText double_encrypt_password = new EditText(UpgradeWalletActivity.this);
                    double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    new AlertDialog.Builder(UpgradeWalletActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.enter_double_encryption_pw)
                            .setView(double_encrypt_password)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    final String pw = double_encrypt_password.getText().toString();

                                    if (DoubleEncryptionFactory.getInstance().validateSecondPassword(
                                            PayloadFactory.getInstance().get().getDoublePasswordHash(),
                                            PayloadFactory.getInstance().get().getSharedKey(),
                                            new CharSequenceX(pw),
                                            PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations())) {

                                        PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));

                                        doUpgrade(new CharSequenceX(pw));
                                    } else {
                                        ToastCustom.makeText(getApplicationContext(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                        PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                    }
                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ;
                        }
                    }).show();
                } else {
                    doUpgrade(new CharSequenceX(""));
                }
            }
        });

        alertDialog.show();
    }

    private void doUpgrade(final CharSequenceX secondPassword) {
        PrefsUtil.getInstance(UpgradeWalletActivity.this).setValue(PrefsUtil.KEY_ASK_LATER, false);

        onUpgradeStart();

        new AsyncTask<Void, Void, Void>(){

            private boolean success = false;

            @Override
            protected Void doInBackground(Void[] params) {
                try {
                    if (ConnectivityStatus.hasConnectivity(UpgradeWalletActivity.this)) {
                        AppUtil.getInstance(UpgradeWalletActivity.this).setUpgradeReminder(System.currentTimeMillis());
                        AppUtil.getInstance(getApplicationContext()).setNewlyCreated(true);
                        boolean isSuccessful = HDPayloadBridge.getInstance(getApplicationContext()).update(PayloadFactory.getInstance().getTempPassword(), secondPassword);
                        if(isSuccessful){
                            PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(getResources().getString(R.string.default_wallet_name));
                            success = true;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if(success){
                    onUpgradeCompleted();
                }else{
                    onUpgradeFail();
                }
            }
        }.execute();
    }

    private void onUpgradeStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heading.setText(getString(R.string.upgrading));
                info.setText(getString(R.string.upgrading_started_info));
                progressBar.setVisibility(View.VISIBLE);
                confirmCancel.setVisibility(View.INVISIBLE);
                confirmUpgrade.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void onUpgradeCompleted() {

        PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L);

        PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heading.setText(getString(R.string.upgrade_success_heading));
                info.setText(getString(R.string.upgrade_success_info));
                progressBar.setVisibility(View.GONE);
                confirmUpgrade.setVisibility(View.VISIBLE);
                confirmUpgrade.setText(getString(R.string.CLOSE));
                confirmUpgrade.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();

                        PrefsUtil.getInstance(UpgradeWalletActivity.this).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                        PrefsUtil.getInstance(UpgradeWalletActivity.this).setValue(PrefsUtil.KEY_ASK_LATER, false);
                        AccessFactory.getInstance(UpgradeWalletActivity.this).setIsLoggedIn(true);
                        AppUtil.getInstance(UpgradeWalletActivity.this).restartApp("verified", true);
                    }
                });
            }
        });
    }

    private void onUpgradeFail() {

        AppUtil.getInstance(getApplicationContext()).setNewlyCreated(false);
        PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L);

        PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heading.setText(getString(R.string.upgrade_fail_heading));
                info.setText(getString(R.string.upgrade_fail_info));
                progressBar.setVisibility(View.GONE);
                confirmUpgrade.setVisibility(View.VISIBLE);
                confirmUpgrade.setText(getString(R.string.CLOSE));
                confirmUpgrade.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
                        AppUtil.getInstance(UpgradeWalletActivity.this).restartApp();
                    }
                });
            }
        });
    }

    public void askLaterClicked(View view) {
        AppUtil.getInstance(this).setUpgradeReminder(System.currentTimeMillis());
        PrefsUtil.getInstance(UpgradeWalletActivity.this).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
        PrefsUtil.getInstance(UpgradeWalletActivity.this).setValue(PrefsUtil.KEY_ASK_LATER, true);
        AccessFactory.getInstance(UpgradeWalletActivity.this).setIsLoggedIn(true);
        AppUtil.getInstance(UpgradeWalletActivity.this).restartApp("verified", true);
    }

    private void setSelectedPage(int position) {

        pageBox0.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));
        pageBox1.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));
        pageBox2.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_dark_blue));

        switch (position) {
            case 0:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_2));
                pageBox0.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));
                break;
            case 1:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_3));
                pageBox1.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));
                break;
            case 2:
                pageHeader.setText(getResources().getString(R.string.upgrade_page_1));
                pageBox2.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_view_upgrade_wallet_blue));
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            AppUtil.getInstance(UpgradeWalletActivity.this).restartApp();

            return true;
        } else {
            ;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    class CustomPagerAdapter extends PagerAdapter {

        Context mContext;
        LayoutInflater mLayoutInflater;
        int[] mResources = {
                R.drawable.upgrade_backup_hilite,
                R.drawable.upgrade_tx_list_hilite,
                R.drawable.upgrade_myaccounts_hilite
        };

        public CustomPagerAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
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
}