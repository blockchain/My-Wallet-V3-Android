package info.blockchain.wallet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.service.WebSocketService;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.ToastCustom;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;

public class AccountEditActivity extends AppCompatActivity {

    private final int SCAN_PRIVX = 302;
    private final int ADDRESS_LABEL_MAX_LENGTH = 32;

    private TextView tvLabelTitle = null;
    private TextView tvLabel = null;

    private TextView tvDefault = null;

    private TextView tvXpub = null;
    private TextView tvExtendedXpubDescription = null;

    private TextView tvArchiveHeading = null;
    private TextView tvArchiveDescription = null;

    private Account account = null;
    private LegacyAddress legacyAddress = null;

    private int accountIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_edit);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        initToolbar();
        getIntentData();
        setupViews();

        updateAllFields();
    }

    private void setupViews() {

        tvLabelTitle = (TextView) findViewById(R.id.tv_label);
        if(account != null){
            tvLabelTitle.setText(getString(R.string.name));//V3
        }else{
            tvLabelTitle.setText(getString(R.string.label));//V2
        }

        tvLabel = (TextView) findViewById(R.id.account_name);
        tvDefault = (TextView) findViewById(R.id.tv_default);

        tvXpub = (TextView)findViewById(R.id.tv_xpub);
        tvExtendedXpubDescription = (TextView)findViewById(R.id.tv_extended_xpub_description);

        tvArchiveHeading = (TextView) findViewById(R.id.tv_archive);
        tvArchiveDescription = (TextView) findViewById(R.id.tv_archive_description);

        if (account != null) {

            findViewById(R.id.privx_container).setVisibility(View.GONE);

        }else if (legacyAddress != null){

            if(legacyAddress.isWatchOnly()){
                findViewById(R.id.privx_container).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.privx_container).setVisibility(View.GONE);
            }
        }
    }

    private void updateAllFields(){
        updateTransferField();
        updateLabelField();
        updateDefaultField();
        updateArchivedField();
        updateXpubField();
    }

    private void updateTransferField(){
        if (account != null) {
            findViewById(R.id.transfer_container).setVisibility(View.GONE);
        }else if (legacyAddress != null && PayloadFactory.getInstance().get().isUpgraded()){

            long balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
            //Subtract fee
            long balanceAfterFee = (balance - SendCoins.bFee.longValue());

            if(balanceAfterFee > SendCoins.bDust.longValue() && !legacyAddress.isWatchOnly()){
                findViewById(R.id.transfer_container).setVisibility(View.VISIBLE);
            }else{
                //No need to show 'transfer' if funds are less than dust amount
                findViewById(R.id.transfer_container).setVisibility(View.GONE);
            }
        }else{
            //No transfer option for V2
            findViewById(R.id.transfer_container).setVisibility(View.GONE);
        }
    }

    private void updateLabelField(){
        if (account != null) {
            tvLabel.setText(account.getLabel());
        }else if (legacyAddress != null){
            tvLabel.setText(legacyAddress.getLabel());
        }
    }

    private void updateDefaultField(){
        if (account != null) {
            if(isDefault(account)){
                tvDefault.setText(getString(R.string.default_label));
                tvDefault.setTextColor(getResources().getColor(R.color.primary_dark_material_dark));
                findViewById(R.id.default_container).setClickable(false);
            }else{
                tvDefault.setText(getString(R.string.make_default));
                tvDefault.setTextColor(getResources().getColor(R.color.blockchain_blue));
                findViewById(R.id.default_container).setClickable(true);
            }

        }else if (legacyAddress != null){
            findViewById(R.id.default_container).setVisibility(View.GONE);//No default for V2
        }
    }

    private void updateArchivedField(){

        if (account != null) {
            setArchive(account.isArchived());
        }else if (legacyAddress != null){
            setArchive(legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS);
        }
    }

    private void updateXpubField(){
        if (account != null) {

            tvXpub.setText(R.string.extended_public_key);
        }else if (legacyAddress != null){

            tvXpub.setText(R.string.address);
            tvExtendedXpubDescription.setVisibility(View.GONE);
        }
    }

    private boolean isDefault(Account account){

        //TODO account.getRealIdx() always returns -1
//        if(account.getRealIdx() == PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex())

        int defaultIndex = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
        List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();

        int accountIndex = 0;
        for(Account acc : accounts){

            if(acc.getXpub().equals(account.getXpub())){
                this.accountIndex = accountIndex;//sets this account index

                if(accountIndex == defaultIndex){//this is current default already
                    return true;
                }
            }

            accountIndex++;
        }
        return false;
    }

    private void toggleArchived(){
        if (account != null) {
                account.setArchived(!account.isArchived());
        } else {
            if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                legacyAddress.setTag(PayloadFactory.NORMAL_ADDRESS);
            } else {
                legacyAddress.setTag(PayloadFactory.ARCHIVED_ADDRESS);
            }
        }
    }

    private void getIntentData() {

        int accountIndex = getIntent().getIntExtra("account_index", -1);
        int addressIndex = getIntent().getIntExtra("address_index", -1);

        if (accountIndex >= 0) {

            //V3
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();

            //Remove "All"
            List<Account> accountClone = new ArrayList<Account>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            account = accountClone.get(accountIndex);

        } else if (addressIndex >= 0) {

            //V2
            ImportedAccount iAccount = null;
            if (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
                iAccount = new ImportedAccount(getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            }
            if (iAccount != null) {

                List<LegacyAddress> legacy = iAccount.getLegacyAddresses();
                legacyAddress = legacy.get(addressIndex);
            }
        }
    }

    private void initToolbar() {

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.edit));
        setSupportActionBar(toolbar);
    }

    private void setArchive(boolean isArchived){

        if(isArchived){
            tvArchiveHeading.setText(R.string.unarchive);
            tvArchiveDescription.setText(R.string.archived_description);

            findViewById(R.id.label_container).setAlpha(0.5f);
            findViewById(R.id.label_container).setClickable(false);
            findViewById(R.id.xpub_container).setAlpha(0.5f);
            findViewById(R.id.xpub_container).setClickable(false);
            findViewById(R.id.privx_container).setAlpha(0.5f);
            findViewById(R.id.privx_container).setClickable(false);
            findViewById(R.id.default_container).setAlpha(0.5f);
            findViewById(R.id.default_container).setClickable(false);
            findViewById(R.id.transfer_container).setAlpha(0.5f);
            findViewById(R.id.transfer_container).setClickable(false);
        }else{

            //Don't allow archiving of default account
            if(isArchivable()){
                findViewById(R.id.archive_container).setAlpha(1.0f);
                findViewById(R.id.archive_container).setClickable(true);
                tvArchiveDescription.setText(R.string.not_archived_description);
            }else{
                findViewById(R.id.archive_container).setAlpha(0.5f);
                findViewById(R.id.archive_container).setClickable(false);
                tvArchiveDescription.setText(getString(R.string.default_account_description));
            }

            tvArchiveHeading.setText(R.string.archive);

            findViewById(R.id.label_container).setAlpha(1.0f);
            findViewById(R.id.label_container).setClickable(true);
            findViewById(R.id.xpub_container).setAlpha(1.0f);
            findViewById(R.id.xpub_container).setClickable(true);
            findViewById(R.id.privx_container).setAlpha(1.0f);
            findViewById(R.id.privx_container).setClickable(true);
            findViewById(R.id.default_container).setAlpha(1.0f);
            findViewById(R.id.default_container).setClickable(true);
            findViewById(R.id.transfer_container).setAlpha(1.0f);
            findViewById(R.id.transfer_container).setClickable(true);
        }
    }
    
    private boolean isArchivable(){

        if (PayloadFactory.getInstance().get().isUpgraded()) {
            //V3 - can't archive default account
            int defaultIndex = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
            Account defaultAccount = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(defaultIndex);

            if(defaultAccount == account)
                return false;
        }else{
            //V2 - must have a single unarchived address
            List<LegacyAddress> allActiveLegacyAddresses = PayloadFactory.getInstance().get().getActiveLegacyAddresses();
            return (allActiveLegacyAddresses.size() > 1);
        }

        return true;
    }

    private void showXpubSharingWarning(){

        new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.xpub_sharing_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        showAddressDetails();
                    }

                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void showAddressDetails(){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_show_extended_public_key, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        TextView tvHeading = (TextView)dialogView.findViewById(R.id.tv_warning_heading);
        TextView tvXpubNote = (TextView)dialogView.findViewById(R.id.tv_xpub_note);
        final TextView tvXpub = (TextView)dialogView.findViewById(R.id.tv_extended_xpub);
        ImageView ivQr = (ImageView)dialogView.findViewById(R.id.iv_qr);

        String qrString = null;

        if (account != null) {

            tvHeading.setText(R.string.extended_public_key);
            tvXpubNote.setText(R.string.scan_this_code);
            tvXpub.setText(R.string.copy_xpub);
            tvXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
            qrString = account.getXpub();

        }else if (legacyAddress != null){

            tvHeading.setText(R.string.address);
            tvXpubNote.setText(legacyAddress.getAddress());
            tvXpub.setText(R.string.copy_address);
            tvXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
            qrString = legacyAddress.getAddress();
        }

        final String finalQrString = qrString;
        tvXpub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) AccountEditActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = null;
                clip = android.content.ClipData.newPlainText("Send address", finalQrString);
                ToastCustom.makeText(AccountEditActivity.this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                clipboard.setPrimaryClip(clip);
            }
        });

        Bitmap bitmap = null;
        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        ivQr.setImageBitmap(bitmap);

        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.cancel();
                }
            }
        });

        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    public void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    public void changeLabelClicked(View view) {

        final EditText etLabel = new EditText(this);
        etLabel.setInputType(InputType.TYPE_CLASS_TEXT);
        etLabel.setPadding(46, 16, 46, 16);
        new AlertDialog.Builder(this)
                .setTitle(R.string.name)
                .setMessage(R.string.assign_display_name)
                .setView(etLabel)
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                if (!ConnectivityStatus.hasConnectivity(AccountEditActivity.this)) {
                                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } else {
                                    if (etLabel != null && etLabel.getText().toString().trim().length() > 0) {

                                        String newLabel = etLabel.getText().toString().trim();

                                        new AsyncTask<String, Void, Void>() {

                                            ProgressDialog progress;

                                            @Override
                                            protected void onPreExecute() {
                                                super.onPreExecute();
                                                progress = new ProgressDialog(AccountEditActivity.this);
                                                progress.setTitle(R.string.app_name);
                                                progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                                                progress.show();
                                            }

                                            @Override
                                            protected void onPostExecute(Void aVoid) {
                                                super.onPostExecute(aVoid);
                                                if (progress != null && progress.isShowing()) {
                                                    progress.dismiss();
                                                    progress = null;
                                                }
                                            }

                                            @Override
                                            protected Void doInBackground(final String... params) {
                                                String revertLabel = null;
                                                if (account != null) {
                                                    revertLabel = account.getLabel();
                                                    account.setLabel(params[0]);
                                                } else {
                                                    revertLabel = legacyAddress.getLabel();
                                                    legacyAddress.setLabel(params[0]);
                                                }

                                                if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            updateLabelField();
                                                            setResult(RESULT_OK);
                                                        }
                                                    });
                                                } else {
                                                    //Remote save not successful - revert
                                                    if (account != null) {
                                                        account.setLabel(revertLabel);
                                                    } else {
                                                        legacyAddress.setLabel(revertLabel);
                                                    }
                                                }
                                                return null;
                                            }
                                        }.execute(newLabel);


                                    } else {
                                        ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.label_cant_be_empty), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }
                                }
                            }
                        }
                ).
                setNegativeButton(R.string.cancel, null)
                .show();

    }

    public void extendedXPubClicked(View view) {
        if (account != null) {
            showXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    public void archiveClicked(View view) {

        String title = getResources().getString(R.string.archive);
        String subTitle = getResources().getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived()) || (legacyAddress != null && legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)) {
            title = getResources().getString(R.string.unarchive);
            subTitle = getResources().getString(R.string.unarchive_are_you_sure);
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(subTitle)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                if (!ConnectivityStatus.hasConnectivity(AccountEditActivity.this)) {
                                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } else {

                                    new AsyncTask<Void, Void, Void>() {

                                        ProgressDialog progress;

                                        @Override
                                        protected void onPreExecute() {
                                            super.onPreExecute();
                                            progress = new ProgressDialog(AccountEditActivity.this);
                                            progress.setTitle(R.string.app_name);
                                            progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                                            progress.setCancelable(false);
                                            progress.show();
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            super.onPostExecute(aVoid);
                                            if (progress != null && progress.isShowing()) {
                                                progress.dismiss();
                                                progress = null;
                                            }
                                        }

                                        @Override
                                        protected Void doInBackground(final Void... params) {

                                            toggleArchived();

                                            if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                                                try {
                                                    HDPayloadBridge.getInstance().updateBalancesAndTransactions();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        updateAllFields();
                                                        setResult(RESULT_OK);
                                                    }
                                                });
                                            }
                                            return null;
                                        }
                                    }.execute();
                                }
                            }
                        }
                ).
                setNegativeButton(R.string.no, null)
                .show();
    }

    public void defaultClicked(View view) {

        new AsyncTask<String, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(AccountEditActivity.this);
                progress.setTitle(R.string.app_name);
                progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
            }

            @Override
            protected Void doInBackground(final String... params) {

                final int revertDefault = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
                PayloadFactory.getInstance().get().getHdWallet().setDefaultIndex(accountIndex);

                if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            updateAllFields();
                            setResult(RESULT_OK);
                        }
                    });
                } else {
                    //Remote save not successful - revert
                    PayloadFactory.getInstance().get().getHdWallet().setDefaultIndex(revertDefault);
                }
                return null;
            }
        }.execute();

    }

    public void scanXPrivClicked(View view) {

        if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.privx_required)
                    .setMessage(getString(R.string.watch_only_spend_instructions).replace("[--address--]", legacyAddress.getAddress()))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            final EditText password = new EditText(AccountEditActivity.this);
                            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                            new AlertDialog.Builder(AccountEditActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.enter_double_encryption_pw)
                                    .setView(password)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            final String pw = password.getText().toString();

                                            if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), new CharSequenceX(pw), PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations())) {

                                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));
                                                Intent intent = new Intent(AccountEditActivity.this, CaptureActivity.class);
                                                startActivityForResult(intent, SCAN_PRIVX);

                                            } else {
                                                ToastCustom.makeText(AccountEditActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                            }

                                        }
                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                        }
                    }).setNegativeButton(R.string.cancel, null).show();
        }else{
            Intent intent = new Intent(AccountEditActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_PRIVX);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK){
            String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);

            try {
                String format = PrivateKeyFactory.getInstance().getFormat(scanData);
                if (format != null) {
                    if (!format.equals(PrivateKeyFactory.BIP38)) {
                        importNonBIP38Address(format, scanData);
                    } else {
                        importBIP38Address(scanData);
                    }
                } else {
                    ToastCustom.makeText(this, getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void importBIP38Address(final String data) {

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

                        new AsyncTask<Void, Void, Void>(){

                            ProgressDialog progress;

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                progress = new ProgressDialog(AccountEditActivity.this);
                                progress.setTitle(R.string.app_name);
                                progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                                progress.show();
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                if (progress != null && progress.isShowing()) {
                                    progress.dismiss();
                                    progress = null;
                                }
                            }

                            @Override
                            protected Void doInBackground(Void... params) {

                                try {
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
                                    final ECKey key = bip38.decrypt(pw);

                                    if (key != null && key.hasPrivKey()) {

                                        final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                                        if (!legacyAddress.getAddress().equals(keyAddress)) {
                                            //Private key does not match this address - warn user but import nevertheless
                                            importUnmatchedPrivateKey(key);
                                        }else{
                                            importAddressPrivateKey(key);
                                        }

                                    } else {
                                        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.invalid_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }
                                } catch (Exception e) {
                                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                }

                                return null;
                            }
                        }.execute();
                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void importNonBIP38Address(final String format, final String data) {

        new AsyncTask<Void, Void, Void>(){

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(AccountEditActivity.this);
                progress.setTitle(R.string.app_name);
                progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final ECKey key = PrivateKeyFactory.getInstance().getKey(format, data);
                    if (key != null && key.hasPrivKey()) {

                        final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                        if (!legacyAddress.getAddress().equals(keyAddress)) {
                            //Private key does not match this address - warn user but import nevertheless
                            importUnmatchedPrivateKey(key);
                        }else{
                            importAddressPrivateKey(key);
                        }

                    } else {
                        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.invalid_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }

                } catch (Exception e) {
                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }

    private void importAddressPrivateKey(ECKey key){

        //if double encrypted, save encrypted in payload
        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            legacyAddress.setWatchOnly(false);
        } else {
            String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
            legacyAddress.setWatchOnly(false);
        }

        if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

            //Reset double encrypt
            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.privx_container).setVisibility(View.GONE);
                    setResult(RESULT_OK);
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    new AlertDialog.Builder(AccountEditActivity.this)
                            .setTitle(R.string.success)
                            .setMessage(R.string.private_key_successfully_imported)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }).show();
                    Looper.loop();

                }
            }).start();
        }
    }

    private void importUnmatchedPrivateKey(ECKey key){

        if (PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
            //Wallet already contains private key - silently avoid duplicating
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    AlertDialog.Builder alert = new AlertDialog.Builder(AccountEditActivity.this);
                    alert.setTitle(getString(R.string.warning));
                    alert.setMessage(getString(R.string.private_key_successfully_imported)+"\n\n"+getString(R.string.private_key_not_matching_address));
                    alert.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            scanXPrivClicked(null);
                        }
                    });
                    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                    alert.show();
                    Looper.loop();

                }
            }).start();
        }else{
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

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    AlertDialog.Builder alert = new AlertDialog.Builder(AccountEditActivity.this);
                    alert.setTitle(getString(R.string.warning));
                    alert.setMessage(getString(R.string.private_key_successfully_imported)+"\n\n"+getString(R.string.private_key_not_matching_address));
                    alert.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            scanXPrivClicked(null);
                        }
                    });
                    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                    alert.show();
                    Looper.loop();

                }
            }).start();

            remoteSaveUnmatchedPrivateKey(legacyAddress);
        }
    }

    private void remoteSaveUnmatchedPrivateKey(final LegacyAddress legacyAddress){

        Payload updatedPayload = PayloadFactory.getInstance().get();
        List<LegacyAddress> updatedLegacyAddresses = updatedPayload.getLegacyAddresses();
        updatedLegacyAddresses.add(legacyAddress);
        updatedPayload.setLegacyAddresses(updatedLegacyAddresses);
        PayloadFactory.getInstance().set(updatedPayload);

        if (PayloadFactory.getInstance().put()) {

            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
            List<String> legacyAddressList = PayloadFactory.getInstance().get().getLegacyAddressStrings();
            MultiAddrFactory.getInstance().getLegacy(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);

            //Subscribe to new address only if successfully created
            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
            intent.putExtra("address", legacyAddress.getAddress());
            LocalBroadcastManager.getInstance(AccountEditActivity.this).sendBroadcast(intent);
            setResult(RESULT_OK);
        } else {
            ToastCustom.makeText(AccountEditActivity.this, getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    public void transferClicked(View view) {

        //Only funded legacy address' will see this option
        final PendingSpend pendingSpend = new PendingSpend();
        pendingSpend.fromLegacyAddress = legacyAddress;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_transfer_funds, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        //From
        TextView confirmFrom = (TextView) dialogView.findViewById(R.id.confirm_from);
        String label = pendingSpend.fromLegacyAddress.getLabel();
        if(label == null || label.isEmpty())label = pendingSpend.fromLegacyAddress.getAddress();
        confirmFrom.setText(label);

        //To default
        TextView confirmTo = (TextView) dialogView.findViewById(R.id.confirm_to);

        int defaultIndex = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
        Account defaultAccount = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(defaultIndex);
        pendingSpend.destination = getV3ReceiveAddress(defaultIndex);

        String toLabel = defaultAccount.getLabel();
        if(toLabel ==null || toLabel.isEmpty())toLabel = pendingSpend.destination;
        confirmTo.setText(toLabel+" ("+getResources().getString(R.string.default_label)+")");

        //Fee
        TextView confirmFee = (TextView) dialogView.findViewById(R.id.confirm_fee);
        pendingSpend.bigIntFee = SendCoins.bFee;
        confirmFee.setText(MonetaryUtil.getInstance(this).getDisplayAmount(pendingSpend.bigIntFee.longValue()) + " BTC");

        //Total
        long balance = MultiAddrFactory.getInstance().getLegacyBalance(pendingSpend.fromLegacyAddress.getAddress());
        long balanceAfterFee = (balance - SendCoins.bFee.longValue());
        pendingSpend.bigIntAmount = BigInteger.valueOf(balanceAfterFee);
        TextView confirmTotal = (TextView) dialogView.findViewById(R.id.confirm_total_to_send);
        double btc_balance = (((double) balanceAfterFee) / 1e8);
        confirmTotal.setText(MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(this).getDenominatedAmount(btc_balance)) + " " + " BTC");

        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        TextView confirmSend = (TextView) dialogView.findViewById(R.id.confirm_send);
        confirmSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)){
                    if (!PayloadFactory.getInstance().get().isDoubleEncrypted() || DoubleEncryptionFactory.getInstance().isActivated()) {
                        sendPayment(pendingSpend);
                    } else {
                        alertDoubleEncrypted(pendingSpend);
                    }
                }else{
                    //This should never happen
                    ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private class PendingSpend {

        LegacyAddress fromLegacyAddress;
        String destination;
        BigInteger bigIntFee;
        BigInteger bigIntAmount;
    }

    private String getV3ReceiveAddress(int accountIndex) {

        try {
            ReceiveAddress receiveAddress = HDPayloadBridge.getInstance(this).getReceiveAddress(accountIndex);
            return receiveAddress.getAddress();

        } catch (DecoderException | IOException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException | AddressFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void alertDoubleEncrypted(final PendingSpend pendingSpend){
        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_double_encryption_pw)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), new CharSequenceX(pw), PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations())) {

                            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));
                            sendPayment(pendingSpend);

                        } else {
                            ToastCustom.makeText(AccountEditActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void sendPayment(final PendingSpend pendingSpend){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                final UnspentOutputsBundle unspents = SendFactory.getInstance(AccountEditActivity.this).prepareSend(-1, pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, BigInteger.ZERO, null);

                if (unspents != null) {
                    executeSend(pendingSpend, unspents);
                } else {

                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                    ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                Looper.loop();
            }
        }).start();
    }

    private void executeSend(final PendingSpend pendingSpend, final UnspentOutputsBundle unspents){

        final ProgressDialog progress;
        progress = new ProgressDialog(AccountEditActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
        progress.setCancelable(false);
        progress.show();

        SendFactory.getInstance(this).execSend(-1, unspents.getOutputs(), pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, null, false, new OpCallback() {

            public void onSuccess() {
            }

            @Override
            public void onSuccess(final String hash) {

                ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                //Update v2 balance immediately after spend - until refresh from server
                MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThread();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.transfer_container).setVisibility(View.GONE);
                    }
                });

                setResult(RESULT_OK);

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

            public void onFail() {

                //Reset double encrypt for V2
                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

            @Override
            public void onFailPermanently() {

                //Reset double encrypt for V2
                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

        });
    }
}
