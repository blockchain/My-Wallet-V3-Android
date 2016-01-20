package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.TypefaceUtil;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.R;

public class PinEntryActivity extends Activity {

    final int PIN_LENGTH = 4;
    final int maxAttempts = 4;
    String userEnteredPIN = "";
    String userEnteredPINConfirm = null;
    TextView titleView = null;

    TextView pinBox0 = null;
    TextView pinBox1 = null;
    TextView pinBox2 = null;
    TextView pinBox3 = null;

    TextView[] pinBoxArray = null;
    boolean allowExit = true;
    int exitClickCount = 0;
    int exitClickCooldown = 2; // in seconds
    private ProgressDialog progress = null;
    private String strEmail = null;
    private String strPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_pin_entry);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Coming from CreateWalletFragment
        getBundleData();
        if (strPassword != null && strEmail != null) {
            allowExit = false;
            saveLoginAndPassword();
            createWallet();
        }

        // Set title state
        Typeface typeface = TypefaceUtil.getInstance(this).getRobotoTypeface();
        titleView = (TextView) findViewById(R.id.titleBox);
        titleView.setTypeface(typeface);
        if (PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1) {

            titleView.setText(R.string.create_pin);
        } else {
            titleView.setText(R.string.pin_entry);
        }

        pinBox0 = (TextView) findViewById(R.id.pinBox0);
        pinBox1 = (TextView) findViewById(R.id.pinBox1);
        pinBox2 = (TextView) findViewById(R.id.pinBox2);
        pinBox3 = (TextView) findViewById(R.id.pinBox3);

        pinBoxArray = new TextView[PIN_LENGTH];
        pinBoxArray[0] = pinBox0;
        pinBoxArray[1] = pinBox1;
        pinBoxArray[2] = pinBox2;
        pinBoxArray[3] = pinBox3;

        if (!ConnectivityStatus.hasConnectivity(this)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            final String message = getString(R.string.check_connectivity_exit);

            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });

            builder.create().show();
        }

        int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        if (fails >= maxAttempts) {
            ToastCustom.makeText(getApplicationContext(), getString(R.string.pin_4_strikes), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

            PayloadFactory.getInstance().get().stepNumber = 0;

            new AlertDialog.Builder(PinEntryActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.password_or_wipe)
                    .setCancelable(false)
                    .setPositiveButton(R.string.use_password, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            validationDialog();

                        }
                    }).setNegativeButton(R.string.wipe_wallet, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();

                }
            }).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cancelClicked(null);
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    public void onBackPressed() {
        if (allowExit) {
            exitClickCount++;
            if (exitClickCount == 2) {
                AppUtil.getInstance(this).logout();
            } else
                ToastCustom.makeText(this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

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
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void saveLoginAndPassword() {
        PrefsUtil.getInstance(this).setValue(PrefsUtil.KEY_EMAIL, strEmail);
        PayloadFactory.getInstance().setEmail(strEmail);
        PayloadFactory.getInstance().setTempPassword(new CharSequenceX(strPassword));
    }

    private void createWallet() {
        dismissProgressView();

        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getText(R.string.create_wallet) + "...");
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    // New wallet
                    AppUtil.getInstance(PinEntryActivity.this).setNewlyCreated(true);

                    HDPayloadBridge.getInstance(PinEntryActivity.this).createHDWallet(12, "", 1);

                    PayloadFactory.getInstance().get().setUpgraded(true);

                    // Save wallet to server
                    if (!PayloadFactory.getInstance().put()) {
                        ToastCustom.makeText(getApplicationContext(), getApplicationContext().getString(R.string.remote_save_ko),
                                ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                } catch (IOException | MnemonicException.MnemonicLengthException e) {
                    ToastCustom.makeText(getApplicationContext(), getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();
                } finally {
                    dismissProgressView();
                }

                Looper.loop();
            }
        }).start();
    }

    private void getBundleData() {
        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("_email")) {
            strEmail = extras.getString("_email");
        }

        if (extras != null && extras.containsKey("_pw")) {
            strPassword = extras.getString("_pw");
        }
    }

    private void updatePayloadThread(final CharSequenceX pw) {
        final Handler handler = new Handler();

        dismissProgressView();

        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.decrypting_wallet));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    if (HDPayloadBridge.getInstance(PinEntryActivity.this).init(pw)) {
                        PayloadFactory.getInstance().setTempPassword(pw);
                        AppUtil.getInstance(PinEntryActivity.this).setSharedKey(PayloadFactory.getInstance().get().getSharedKey());

                        if (AppUtil.getInstance(PinEntryActivity.this).isNewlyCreated() && PayloadFactory.getInstance().get().getHdWallet() != null &&
                                (PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).getLabel() == null ||
                                        PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).getLabel().isEmpty()))
                            PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(getResources().getString(R.string.default_wallet_name));

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                dismissProgressView();

                                if (PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L) == 0L && !PayloadFactory.getInstance().get().isUpgraded()) {
                                    Intent intent = new Intent(PinEntryActivity.this, UpgradeWalletActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } else {
                                    AppUtil.getInstance(PinEntryActivity.this).restartApp("verified", true);
                                }
                            }
                        });
                    } else {
                        dismissProgressView();
                        AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();
                    }

                    Looper.loop();
                } finally {
                    dismissProgressView();
                }
            }
        }).start();
    }

    private void createPINThread(final String pin) {
        final Handler handler = new Handler();

        dismissProgressView();
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.creating_pin));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if (AccessFactory.getInstance(PinEntryActivity.this).createPIN(PayloadFactory.getInstance().getTempPassword(), pin)) {
                    dismissProgressView();

                    PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    updatePayloadThread(PayloadFactory.getInstance().getTempPassword());

                } else {
                    dismissProgressView();

                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.create_pin_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    PrefsUtil.getInstance(PinEntryActivity.this).clear();
                    AppUtil.getInstance(PinEntryActivity.this).restartApp();
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

    public void validatePIN(final String PIN) {
        validatePINThread(PIN);
    }

    private void validatePINThread(final String pin) {
        final Handler handler = new Handler();

        dismissProgressView();
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.validating_pin));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                CharSequenceX password;

                try {
                    password = AccessFactory.getInstance(PinEntryActivity.this).validatePIN(pin);
                } catch (Exception e) {
                    dismissProgressView();

                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.unexpected_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return;
                }

                if (password != null) {
                    dismissProgressView();

                    PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    updatePayloadThread(password);
                } else {
                    dismissProgressView();

                    incrementFailureCount();
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

    private void validationDialog() {
        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(PinEntryActivity.this.getString(R.string.password_entry))
                .setView(password)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        AppUtil.getInstance(PinEntryActivity.this).restartApp();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (pw.length() > 0) {
                            validatePasswordThread(new CharSequenceX(pw));
                        } else {
                            incrementFailureCount();
                        }

                    }
                }).show();
    }

    private void validatePasswordThread(final CharSequenceX pw) {
        dismissProgressView();
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.validating_password));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    PayloadFactory.getInstance().setTempPassword(new CharSequenceX(""));
                    if (HDPayloadBridge.getInstance(PinEntryActivity.this).init(pw)) {

                        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.pin_4_strikes_password_accepted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                        PayloadFactory.getInstance().setTempPassword(pw);
                        PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.KEY_PIN_FAILS);
                        PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        dismissProgressView();
                        validationDialog();
                    }

                    Looper.loop();
                } finally {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }
            }
        }).start();
    }

    public void padClicked(View view) {
        if (userEnteredPIN.length() == PIN_LENGTH) {
            return;
        }

        // Append tapped #
        userEnteredPIN = userEnteredPIN + view.getTag().toString().substring(0, 1);
        pinBoxArray[userEnteredPIN.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

        // Perform appropriate action if PIN_LENGTH has been reached
        if (userEnteredPIN.length() == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (userEnteredPIN.equals("0000")) {
                ToastCustom.makeText(PinEntryActivity.this, getString(R.string.zero_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clearPinBoxes();
                        userEnteredPIN = "";
                        userEnteredPINConfirm = null;
                    }
                }, 200);
                return;
            }

            // Validate
            if (PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() >= 1) {
                titleView.setVisibility(View.INVISIBLE);
                validatePIN(userEnteredPIN);
            } else if (userEnteredPINConfirm == null) {
                // End of Create -  Change to Confirm
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        PinEntryActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                titleView.setText(R.string.confirm_pin);
                                clearPinBoxes();
                                userEnteredPINConfirm = userEnteredPIN;
                                userEnteredPIN = "";
                            }
                        });
                    }
                }, 200);

            } else if (userEnteredPINConfirm.equals(userEnteredPIN)) {
                // End of Confirm - Pin is confirmed
                createPINThread(userEnteredPIN); // Pin is confirmed. Save to server.

            } else {
                // End of Confirm - Pin Mismatch
                ToastCustom.makeText(PinEntryActivity.this, getString(R.string.pin_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clearPinBoxes();
                        userEnteredPIN = "";
                        userEnteredPINConfirm = null;
                        titleView.setText(R.string.create_pin);
                    }
                }, 200);
            }
        }
    }

    public void cancelClicked(View view) {
        clearPinBoxes();
        userEnteredPIN = "";
    }

    private void clearPinBoxes() {
        if (userEnteredPIN.length() > 0) {
            for (TextView pinBox : pinBoxArray) {
                // Reset PIN buttons to blank
                pinBox.setBackgroundResource(R.drawable.rounded_view_blue_white_border);
            }
        }
    }

    private void incrementFailureCount() {
        int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);

        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void dismissProgressView() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }
}