package info.blockchain.wallet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import info.blockchain.wallet.listeners.RecyclerItemClickListener;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.service.WebSocketService;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.AddressInfo;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.ToastCustom;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

//import android.util.Log;

public class AccountActivity extends AppCompatActivity {

    public static final String ACTION_INTENT = BalanceFragment.ACTION_INTENT;
    private static final int IMPORT_PRIVATE_REQUEST_CODE = 2006;
    private static final int EDIT_ACTIVITY_REQUEST_CODE = 2007;

    private static int ADDRESS_LABEL_MAX_LENGTH = 32;

    private static String[] HEADERS;
    public static String IMPORTED_HEADER;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {

                AccountActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateAccountsList();
                    }
                });

            }
        }
    };
    private LinearLayoutManager layoutManager = null;
    private RecyclerView mRecyclerView = null;
    private ArrayList<AccountItem> accountsAndImportedList = null;
    private AccountAdapter accountsAdapter = null;
    private ArrayList<Integer> headerPositions;
    private int hdAccountsIdx;
    private List<LegacyAddress> legacy = null;
    private ProgressDialog progress = null;
    private Context context = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        setContentView(R.layout.activity_accounts);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        initToolbar();

        IMPORTED_HEADER = getResources().getString(R.string.imported_addresses);

        if (!AppUtil.getInstance(AccountActivity.this).isNotUpgraded())
            HEADERS = new String[]{IMPORTED_HEADER};
        else
            HEADERS = new String[0];

        mRecyclerView = (RecyclerView) findViewById(R.id.accountsList);
        layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        accountsAndImportedList = new ArrayList<>();
        updateAccountsList();
        accountsAdapter = new AccountAdapter(accountsAndImportedList);
        mRecyclerView.setAdapter(accountsAdapter);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(final View view, int position) {

                        if (!AppUtil.getInstance(AccountActivity.this).isNotUpgraded())
                            if (headerPositions.contains(position)) return;//headers unclickable

                        onRowClick(position);
                    }
                })
        );
    }

    private void initToolbar(){

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        if (!AppUtil.getInstance(AccountActivity.this).isNotUpgraded()) {
            toolbar.setTitle("");//TODO - empty header for V3 for now - awaiting product
        } else {
            toolbar.setTitle(getResources().getString(R.string.my_addresses));
        }
        setSupportActionBar(toolbar);
    }

    private void onRowClick(int position){

        Intent intent = new Intent(this, AccountEditActivity.class);
        if (position - HEADERS.length >= hdAccountsIdx) {//2 headers before imported
            intent.putExtra("address_index", position - HEADERS.length - hdAccountsIdx);
        } else {
            intent.putExtra("account_index", position);
        }
        startActivityForResult(intent, EDIT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (!AppUtil.getInstance(AccountActivity.this).isNotUpgraded()) {
            inflater.inflate(R.menu.v3_myaccounts_activity_actions, menu);
        }else {
            inflater.inflate(R.menu.v2_myaccounts_activity_actions, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_import_address:
                importAddress();
                return true;
            case R.id.action_create_new:
                createNewAccount();
                return true;
            case R.id.action_create_new_address:
                createNewAddress();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void importAddress(){

        if (!AppUtil.getInstance(AccountActivity.this).isCameraOpen()) {

            if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                Intent intent = new Intent(AccountActivity.this, CaptureActivity.class);
                intent.putExtra(Intents.Scan.FORMATS, EnumSet.allOf(BarcodeFormat.class));
                intent.putExtra(Intents.Scan.MODE, Intents.Scan.MODE);
                startActivityForResult(intent, IMPORT_PRIVATE_REQUEST_CODE);
            } else {
                final EditText double_encrypt_password = new EditText(AccountActivity.this);
                double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                new AlertDialog.Builder(AccountActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.enter_double_encryption_pw)
                        .setView(double_encrypt_password)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                String pw2 = double_encrypt_password.getText().toString();

                                if (pw2 != null && pw2.length() > 0 && DoubleEncryptionFactory.getInstance().validateSecondPassword(
                                        PayloadFactory.getInstance().get().getDoublePasswordHash(),
                                        PayloadFactory.getInstance().get().getSharedKey(),
                                        new CharSequenceX(pw2),
                                        PayloadFactory.getInstance().get().getOptions().getIterations()
                                )) {

                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw2));

                                    Intent intent = new Intent(AccountActivity.this, CaptureActivity.class);
                                    intent.putExtra(Intents.Scan.FORMATS, EnumSet.allOf(BarcodeFormat.class));
                                    intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
                                    startActivityForResult(intent, IMPORT_PRIVATE_REQUEST_CODE);

                                } else {
                                    ToastCustom.makeText(AccountActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                }

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();
            }

        } else {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void createNewAccount(){
        //TODO - Go to create
    }

    private void createNewAddress(){

        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else {

            if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                addAddress();
            } else {

                final EditText double_encrypt_password = new EditText(AccountActivity.this);
                double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                new AlertDialog.Builder(AccountActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.enter_double_encryption_pw)
                        .setView(double_encrypt_password)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                String pw2 = double_encrypt_password.getText().toString();

                                if (pw2 != null && pw2.length() > 0 && DoubleEncryptionFactory.getInstance().validateSecondPassword(
                                        PayloadFactory.getInstance().get().getDoublePasswordHash(),
                                        PayloadFactory.getInstance().get().getSharedKey(),
                                        new CharSequenceX(pw2),
                                        PayloadFactory.getInstance().get().getOptions().getIterations()
                                )) {

                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw2));

                                    addAddress();

                                } else {
                                    ToastCustom.makeText(AccountActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                }

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

            }

        }

    }

    private void updateAccountsList() {

        headerPositions = new ArrayList<Integer>();

        //accountsAndImportedList is linked to AccountAdapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        accountsAndImportedList.clear();

        int i = 0;
        if (PayloadFactory.getInstance().get().isUpgraded()) {

            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
            List<Account> accountClone = new ArrayList<Account>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            int archivedCount = 0;
            int j = 1;
            for (; i < accountClone.size(); i++) {

                String label = accountClone.get(i).getLabel();
                if (label == null || label.length() == 0) label = "Account: " + (i + 1);
                if (accountClone.get(i).isArchived())
                    label = context.getString(R.string.archived_label) + " " + label;

                j++;
                accountsAndImportedList.add(new AccountItem(label, getAccountBalance(i), getResources().getDrawable(R.drawable.icon_accounthd)));
            }
            hdAccountsIdx = accountClone.size() - archivedCount;
        }

        ImportedAccount iAccount = null;
        if (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
            iAccount = new ImportedAccount(getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        }
        if (iAccount != null) {

            if (!AppUtil.getInstance(AccountActivity.this).isNotUpgraded()) {
                //Imported Header Position
                headerPositions.add(accountsAndImportedList.size());
                accountsAndImportedList.add(new AccountItem(HEADERS[0], "", getResources().getDrawable(R.drawable.icon_accounthd)));
            }

            legacy = iAccount.getLegacyAddresses();
            for (int j = 0; j < legacy.size(); j++) {

                String label = legacy.get(j).getLabel();
                if (label == null || label.length() == 0) label = legacy.get(j).getAddress();

                if (legacy.get(j).isWatchOnly()) {
                    label = context.getString(R.string.watch_only_label) + " " + label;
                } else if (legacy.get(j).getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                    label = context.getString(R.string.archived_label) + " " + label;
                }

                accountsAndImportedList.add(new AccountItem(label, getAddressBalance(j), getResources().getDrawable(R.drawable.icon_imported)));
            }
        }

        AccountActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(accountsAdapter != null)accountsAdapter.notifyDataSetChanged();
            }
        });
    }

    private String getAccountBalance(int index) {

        String address = HDPayloadBridge.getInstance(this).account2Xpub(index);
        Long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(address);
        if (amount == null) amount = 0l;

        String unit = (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

        return MonetaryUtil.getInstance(AccountActivity.this).getDisplayAmount(amount) + " " + unit;
    }

    private String getAddressBalance(int index) {

        String address = legacy.get(index).getAddress();
        Long amount = MultiAddrFactory.getInstance().getLegacyBalance(address);
        if (amount == null) amount = 0l;
        String unit = (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

        return MonetaryUtil.getInstance(AccountActivity.this).getDisplayAmount(amount) + " " + unit;
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(AccountActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(AccountActivity.this).unregisterReceiver(receiver);
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == IMPORT_PRIVATE_REQUEST_CODE
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            try {
                final String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
                String format = PrivateKeyFactory.getInstance().getFormat(strResult);
                if (format != null) {
                    if (!format.equals(PrivateKeyFactory.BIP38)) {
                        importNonBIP38Address(format, strResult);
                    } else {
                        importBIP38Address(strResult);
                    }
                } else {
                    ToastCustom.makeText(AccountActivity.this, getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
            } catch (Exception e) {
                ToastCustom.makeText(AccountActivity.this, getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == IMPORT_PRIVATE_REQUEST_CODE) {
            ;
        } else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {

            updateAccountsList();

        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {

        }
    }

    private void importBIP38Address(final String data) {

        final List<LegacyAddress> rollbackLegacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }
                        progress = new ProgressDialog(AccountActivity.this);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(AccountActivity.this.getResources().getString(R.string.please_wait));
                        progress.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                Looper.prepare();

                                try {
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
                                    final ECKey key = bip38.decrypt(pw);

                                    if (key != null && key.hasPrivKey() && !PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
                                        final LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", "");
                                                    /*
                                                     * if double encrypted, save encrypted in payload
                                                     */
                                        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                                            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
                                        } else {
                                            String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
                                            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
                                            legacyAddress.setEncryptedKey(encrypted2);
                                        }

                                        final EditText address_label = new EditText(AccountActivity.this);
                                        address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

                                        new AlertDialog.Builder(AccountActivity.this)
                                                .setTitle(R.string.app_name)
                                                .setMessage(R.string.label_address)
                                                .setView(address_label)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        String label = address_label.getText().toString();
                                                        if (label != null && label.trim().length() > 0) {
                                                            legacyAddress.setLabel(label);
                                                        } else {
                                                            legacyAddress.setLabel(legacyAddress.getAddress());
                                                        }

                                                        remoteSaveNewAddress(legacyAddress);

                                                    }
                                                }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                legacyAddress.setLabel(legacyAddress.getAddress());
                                                remoteSaveNewAddress(legacyAddress);

                                            }
                                        }).show();

                                    } else {
                                        ToastCustom.makeText(getApplicationContext(), getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                    }
                                } catch (Exception e) {
                                    ToastCustom.makeText(AccountActivity.this, getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } finally {
                                    if (progress != null && progress.isShowing()) {
                                        progress.dismiss();
                                        progress = null;
                                    }
                                }

                                Looper.loop();

                            }
                        }).start();

                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void importNonBIP38Address(final String format, final String data) {

        ECKey key = null;

        try {
            key = PrivateKeyFactory.getInstance().getKey(format, data);
        } catch (Exception e) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            e.printStackTrace();
            return;
        }

        if (key != null && key.hasPrivKey() && PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.address_already_in_wallet), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else if (key != null && key.hasPrivKey() && !PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

            final List<LegacyAddress> rollbackLegacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

            final LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", "");
            /*
             * if double encrypted, save encrypted in payload
             */
            if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            } else {
                String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
                String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
                legacyAddress.setEncryptedKey(encrypted2);
            }

            final EditText address_label = new EditText(AccountActivity.this);
            address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

            final ECKey scannedKey = key;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    new AlertDialog.Builder(AccountActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.label_address)
                            .setView(address_label)
                            .setCancelable(false)
                            .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    String label = address_label.getText().toString();
                                    if (label != null && label.trim().length() > 0) {
                                        legacyAddress.setLabel(label);
                                    } else {
                                        legacyAddress.setLabel(legacyAddress.getAddress());
                                    }

                                    remoteSaveNewAddress(legacyAddress);

                                }
                            }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            legacyAddress.setLabel(legacyAddress.getAddress());
                            remoteSaveNewAddress(legacyAddress);

                        }
                    }).show();

                    Looper.loop();
                }
            }).start();

        } else {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

    }

    private void addAddressAndUpdateList(final LegacyAddress legacyAddress) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                JSONObject info = AddressInfo.getInstance().getAddressInfo(legacyAddress.getAddress());

                long balance = 0l;
                if (info != null)
                    try {
                        balance = info.getLong("final_balance");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                MultiAddrFactory.getInstance().setLegacyBalance(legacyAddress.getAddress(), balance);
                MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() + balance);
                AccountsUtil.getInstance(AccountActivity.this).setCurrentSpinnerIndex(0);

                updateAccountsList();

                Looper.loop();

            }
        }).start();
    }

    private void addAddress() {

        final Handler mHandler = new Handler();

        final ProgressDialog progress = new ProgressDialog(AccountActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.setCancelable(false);

        new AsyncTask<Void, Void, ECKey>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress.show();
            }

            @Override
            protected ECKey doInBackground(Void... params) {

                ECKey ecKey = PayloadBridge.getInstance(AccountActivity.this).newLegacyAddress();
                if (ecKey == null) {
                    ToastCustom.makeText(context, context.getString(R.string.cannot_create_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    return null;
                }
                return ecKey;
            }

            @Override
            protected void onPostExecute(ECKey ecKey) {
                super.onPostExecute(ecKey);

                String encryptedKey = new String(Base58.encode(ecKey.getPrivKeyBytes()));
                if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                    encryptedKey = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
                }
                final LegacyAddress legacyAddress = new LegacyAddress();
                legacyAddress.setEncryptedKey(encryptedKey);
                legacyAddress.setAddress(ecKey.toAddress(MainNetParams.get()).toString());
                legacyAddress.setCreatedDeviceName("android");
                legacyAddress.setCreated(System.currentTimeMillis());
                legacyAddress.setCreatedDeviceVersion(BuildConfig.VERSION_NAME);

                progress.dismiss();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mHandler.post(new Runnable() {

                                @Override
                                public void run() {
                                    final EditText address_label = new EditText(AccountActivity.this);
                                    address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

                                    new AlertDialog.Builder(AccountActivity.this)
                                            .setTitle(R.string.app_name)
                                            .setMessage(R.string.label_address2)
                                            .setView(address_label)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    String label = address_label.getText().toString();
                                                    if (label != null && label.trim().length() > 0) {
                                                        ;
                                                    } else {
                                                        label = legacyAddress.getAddress();
                                                    }

                                                    legacyAddress.setLabel(label);
                                                    remoteSaveNewAddress(legacyAddress);

                                                }
                                            }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            legacyAddress.setLabel(legacyAddress.getAddress());
                                            remoteSaveNewAddress(legacyAddress);

                                        }
                                    }).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }.execute();
    }

    private void remoteSaveNewAddress(final LegacyAddress legacy) {

        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        final ProgressDialog progress = new ProgressDialog(AccountActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.saving_address));
        progress.setCancelable(false);
        progress.show();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                if (PayloadFactory.getInstance().get() != null) {

                    Payload updatedPayload = PayloadFactory.getInstance().get();
                    List<LegacyAddress> updatedLegacyAddresses = updatedPayload.getLegacyAddresses();
                    updatedLegacyAddresses.add(legacy);
                    updatedPayload.setLegacyAddresses(updatedLegacyAddresses);
                    PayloadFactory.getInstance().set(updatedPayload);

                    if (PayloadFactory.getInstance().put()) {
                        ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.remote_save_ok), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                        ToastCustom.makeText(getApplicationContext(), legacy.getAddress(), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

                        PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                        List<String> legacyAddressList = PayloadFactory.getInstance().get().getLegacyAddressStrings();
                        MultiAddrFactory.getInstance().getLegacy(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
                        AccountsUtil.getInstance(context).initAccountMaps();

                        //Subscribe to new address only if successfully created
                        Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                        intent.putExtra("address", legacy.getAddress());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                        addAddressAndUpdateList(legacy);
                    } else {
                        ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        AppUtil.getInstance(AccountActivity.this).restartApp();
                    }
                } else {
//                    ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.payload_corrupted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    AppUtil.getInstance(AccountActivity.this).restartApp();
                }

                progress.dismiss();

                return null;
            }
        }.execute();
    }
}
