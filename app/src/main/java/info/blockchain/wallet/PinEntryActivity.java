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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.TypefaceUtil;
import piuk.blockchain.android.R;

//import libsrc.org.apache.commons.io.IOUtils;

public class PinEntryActivity extends Activity {

    String userEnteredPIN = "";
    String userEnteredPINConfirm = null;

    final int PIN_LENGTH = 4;
    final int maxAttempts = 4;

    TextView titleView = null;

    TextView pinBox0 = null;
    TextView pinBox1 = null;
    TextView pinBox2 = null;
    TextView pinBox3 = null;

    TextView[] pinBoxArray = null;

    private ProgressDialog progress = null;

    private String strEmail    = null;
    private String strPassword = null;

    int exitClickCount = 0;
    int exitClickCooldown = 2;//seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_pin_entry);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().getDecorView().findViewById(android.R.id.content).setFilterTouchesWhenObscured(true);

        //Coming from CreateWalletFragment
        getBundleData();
        if (strPassword != null && strEmail != null) {
            saveLoginAndPassword();
            createWallet();
        }

        // Set title state
        Typeface typeface = TypefaceUtil.getInstance(this).getRobotoTypeface();
        titleView = (TextView)findViewById(R.id.titleBox);
        titleView.setTypeface(typeface);
        if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1) {

            titleView.setText(R.string.create_pin);
        }
        else {
            titleView.setText(R.string.pin_entry);
        }

        pinBox0 = (TextView)findViewById(R.id.pinBox0);
        pinBox1 = (TextView)findViewById(R.id.pinBox1);
        pinBox2 = (TextView)findViewById(R.id.pinBox2);
        pinBox3 = (TextView)findViewById(R.id.pinBox3);

        pinBoxArray = new TextView[PIN_LENGTH];
        pinBoxArray[0] = pinBox0;
        pinBoxArray[1] = pinBox1;
        pinBoxArray[2] = pinBox2;
        pinBoxArray[3] = pinBox3;

        if(!ConnectivityStatus.hasConnectivity(this)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            final String message = getString(R.string.check_connectivity_exit);

            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.dismiss();
                                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });

            builder.create().show();
        }

        int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        if(fails >= maxAttempts)	{
            ToastCustom.makeText(getApplicationContext(), getString(R.string.pin_4_strikes), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
//        	validationDialog();

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

    private void saveLoginAndPassword() {
        PrefsUtil.getInstance(this).setValue(PrefsUtil.KEY_EMAIL, strEmail);
        PayloadFactory.getInstance().setEmail(strEmail);
        PayloadFactory.getInstance().setTempPassword(new CharSequenceX(strPassword));
    }

    private void createWallet() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                if(AppUtil.getInstance(PinEntryActivity.this).isLegacy())    {
                    AppUtil.getInstance(PinEntryActivity.this).setNewlyCreated(true);

                    String guid = UUID.randomUUID().toString();
                    String sharedKey = UUID.randomUUID().toString();

                    Payload payload = new Payload();
                    payload.setGuid(guid);
                    payload.setSharedKey(sharedKey);

                    PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_GUID, guid);
                    AppUtil.getInstance(PinEntryActivity.this).setSharedKey(sharedKey);

                    ECKey ecKey = PayloadBridge.getInstance(PinEntryActivity.this).newLegacyAddress();
                    if(ecKey == null)    {
                        ToastCustom.makeText(PinEntryActivity.this, PinEntryActivity.this.getString(R.string.cannot_create_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();
                        return;
                    }
                    String encryptedKey = new String(Base58.encode(ecKey.getPrivKeyBytes()));

                    LegacyAddress legacyAddress = new LegacyAddress();
                    legacyAddress.setEncryptedKey(encryptedKey);
                    legacyAddress.setAddress(ecKey.toAddress(MainNetParams.get()).toString());
                    legacyAddress.setLabel(PinEntryActivity.this.getString(R.string.my_address));
                    legacyAddress.setCreatedDeviceName("android");
                    legacyAddress.setCreated(System.currentTimeMillis());
                    legacyAddress.setCreatedDeviceVersion(PinEntryActivity.this.getString(R.string.version_name));
                    List<LegacyAddress> legacyAddresses = new ArrayList<LegacyAddress>();
                    legacyAddresses.add(legacyAddress);

                    payload.setLegacyAddresses(legacyAddresses);

                    PayloadFactory.getInstance().set(payload);
                    PayloadFactory.getInstance().setNew(true);

                    PayloadBridge.getInstance(PinEntryActivity.this).remoteSaveThread();

                    if(AppUtil.getInstance(PinEntryActivity.this).isLegacy())    {
                        ;
                    }
                    else    {
                        whitelistGuid("alpha");// <-- remove after beta invite system
                        whitelistGuid("dev");// <-- remove after beta invite system
                    }
//            AppUtil.getInstance(this).restartApp();// <-- put back after beta invite system
                }
                else    {
                    try {
                        // create wallet
                        // restart

//            PrefsUtil.getInstance(this).setValue(PrefsUtil.KEY_HD_ISUPGRADED, true);
                        AppUtil.getInstance(PinEntryActivity.this).setNewlyCreated(true);

                        HDPayloadBridge.getInstance(PinEntryActivity.this).createHDWallet(12, "", 1);

                        PayloadBridge.getInstance(PinEntryActivity.this).remoteSaveThread();

                        if(AppUtil.getInstance(PinEntryActivity.this).isLegacy())    {
                            ;
                        }
                        else    {
                            whitelistGuid("alpha");// <-- remove after beta invite system
                            whitelistGuid("dev");// <-- remove after beta invite system
                        }
//            AppUtil.getInstance(this).restartApp();// <-- put back after beta invite system

                    } catch (IOException | MnemonicException.MnemonicLengthException e) {
                        ToastCustom.makeText(getApplicationContext(), getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();
                    }

                }

                Looper.loop();

            }
        }).start();

    }

    private void whitelistGuid(final String domain) {

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage("Registering for ALPHA...");
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                InputStream is = null;
                OutputStream os = null;

                URL url = null;
                try {

                    url = new URL("https://"+domain+".blockchain.info/whitelist_guid/");
                    JSONObject json = new JSONObject();
                    json.put("secret", "HvWJeR1WdybHvq0316i");
                    json.put("guid", PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_GUID, ""));
                    json.put("email", PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_EMAIL, ""));
                    String message = json.toString();

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    try {
                        conn.setReadTimeout(60000);
                        conn.setConnectTimeout(60000);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setFixedLengthStreamingMode(message.getBytes().length);
                        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                        conn.connect();

                        os = new BufferedOutputStream(conn.getOutputStream());
                        os.write(message.getBytes());
                        os.flush();

                        if(progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }

                        if (conn.getResponseCode() == 200) {
                            ToastCustom.makeText(getApplicationContext(), "Successfully registered", ToastCustom.LENGTH_LONG, ToastCustom.TYPE_OK);
                        } else {
                            ToastCustom.makeText(getApplicationContext(), "Error: " + IOUtils.toString(conn.getErrorStream(), "UTF-8"), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }
                    } finally {
                        if (os != null) os.close();
                        if (is != null) is.close();
                        conn.disconnect();

                        if(progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Looper.loop();
            }
        }
        ).start();
    }

    private void getBundleData() {

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("_email")) {
            strEmail = extras.getString("_email");
        }

        if (extras != null && extras.containsKey("_pw")) {
            strPassword = extras.getString("_pw");
        }

        if (extras != null && extras.containsKey(PairingFactory.KEY_EXTRA_IS_PAIRING))
            AppUtil.getInstance(this).restartApp(); // ?
    }


    @Override
    public void onBackPressed()
    {
        exitClickCount++;
        if(exitClickCount ==2) {
            AppUtil.getInstance(this).clearUserInteractionTime();
            finish();
        }else
            ToastCustom.makeText(this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for(int j = 0; j <= exitClickCooldown; j++)
                {
                    try{Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
                    if(j >= exitClickCooldown) exitClickCount = 0;
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void validatePIN(final String PIN) {
        validatePINThread(PIN);
    }

    private void updatePayloadThread(final CharSequenceX pw) {

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
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
                    if(HDPayloadBridge.getInstance(PinEntryActivity.this).init(pw)) {

                        if(AppUtil.getInstance(PinEntryActivity.this).isLegacy() && PayloadFactory.getInstance().getVersion() >= 3.0){
                            ToastCustom.makeText(PinEntryActivity.this,getString(R.string.lame_mode_hd_pair_fail),ToastCustom.LENGTH_SHORT,ToastCustom.TYPE_ERROR);
                            AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();
                            return;
                        }

                        PayloadFactory.getInstance().setTempPassword(pw);
                        AppUtil.getInstance(PinEntryActivity.this).setSharedKey(PayloadFactory.getInstance().get().getSharedKey());
                        AppUtil.getInstance(PinEntryActivity.this).initUserInteraction();

                        if(AppUtil.getInstance(PinEntryActivity.this).isNewlyCreated() && PayloadFactory.getInstance().get().getHdWallet()!=null && (PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).getLabel()==null || PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).getLabel().isEmpty()))
                            PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(getResources().getString(R.string.default_wallet_name));


                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                if(progress != null && progress.isShowing()) {
                                    progress.dismiss();
                                    progress = null;
                                }

                                if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L) == 0L && !PayloadFactory.getInstance().get().isUpgraded())    {

                                    if(AppUtil.getInstance(PinEntryActivity.this).isLegacy())    {
                                        AppUtil.getInstance(PinEntryActivity.this).setUpgradeReminder(System.currentTimeMillis());
                                        PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_ASK_LATER, true);
                                        AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                                        AppUtil.getInstance(PinEntryActivity.this).restartApp("verified", true);
                                    }
                                    else    {
                                        Intent intent = new Intent(PinEntryActivity.this, UpgradeWalletActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }

                                }
                                else  {
                                    AppUtil.getInstance(PinEntryActivity.this).restartApp("verified", true);
                                }
                            }

                        });

                    }
                    else {
                        if(progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }
                        AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();
                    }

                    Looper.loop();

                }
                catch(JSONException | IOException | DecoderException | AddressFormatException
                        | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException
                        | MnemonicException.MnemonicChecksumException e) {
                    e.printStackTrace();
                }
                finally {
                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

            }
        }).start();
    }

    private void createPINThread(final String pin) {

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.creating_pin));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if(AccessFactory.getInstance(PinEntryActivity.this).createPIN(PayloadFactory.getInstance().getTempPassword(), pin)) {

                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    updatePayloadThread(PayloadFactory.getInstance().getTempPassword());

                }
                else {

                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

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

    private void validatePINThread(final String pin) {

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.validating_pin));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                CharSequenceX password = null;

                try {
                    password = AccessFactory.getInstance(PinEntryActivity.this).validatePIN(pin);
                }catch (Exception e){
                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.unexpected_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return;
                }

                if(password != null) {

                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    AppUtil.getInstance(PinEntryActivity.this).setIsLocked(false);
                    updatePayloadThread(password);
                }
                else {

                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    incFailure();

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

    private void validationDialog()	{

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

                        if (pw != null && pw.length() > 0) {
                            validatePasswordThread(new CharSequenceX(pw));
                        } else {
                            incFailure();
                        }

                    }
                }).show();

    }

    private void validatePasswordThread(final CharSequenceX pw) {

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
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
                    if(HDPayloadBridge.getInstance(PinEntryActivity.this).init(pw)) {

                        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.pin_4_strikes_password_accepted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                        PayloadFactory.getInstance().setTempPassword(pw);
                        PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.KEY_PIN_FAILS);
                        PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    else {
                        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        if(progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }

                        validationDialog();

                    }

                    Looper.loop();

                }
                catch(JSONException | IOException | DecoderException | AddressFormatException
                        | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException
                        | MnemonicException.MnemonicChecksumException e) {
                    e.printStackTrace();
                }
                finally {
                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

            }
        }).start();
    }

    public void padClicked(View view) {

        if(userEnteredPIN.length() == PIN_LENGTH) {
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
                clearPinBoxes();
                userEnteredPIN = "";
                userEnteredPINConfirm = null;
                return;
            }

            // Validate
            if (PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() >= 1) {
                titleView.setVisibility(View.INVISIBLE);
                validatePIN(userEnteredPIN);
            }

            else if(userEnteredPINConfirm == null)
            {
                //End of Create -  Change to Confirm
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

            }else if (userEnteredPINConfirm != null && userEnteredPINConfirm.equals(userEnteredPIN))
            {
                //End of Confirm - Pin is confirmed
                createPINThread(userEnteredPIN); // Pin is confirmed. Save to server.

            } else {

                //End of Confirm - Pin Mismatch
                ToastCustom.makeText(PinEntryActivity.this, getString(R.string.pin_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                clearPinBoxes();
                userEnteredPIN = "";
                userEnteredPINConfirm = null;
                titleView.setText(R.string.create_pin);
            }
        }
    }

    public void cancelClicked(View view) {
        clearPinBoxes();
        userEnteredPIN = "";
    }

    private void clearPinBoxes(){
        if(userEnteredPIN.length() > 0)	{
            for(int i = 0; i < pinBoxArray.length; i++)	{
                pinBoxArray[i].setBackgroundResource(R.drawable.rounded_view_blue_white_border);//reset pin buttons blank
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).setIsLocked(true);
        cancelClicked(null);
    }

    //
    // increment failure count
    //
    private void incFailure()   {
        int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}