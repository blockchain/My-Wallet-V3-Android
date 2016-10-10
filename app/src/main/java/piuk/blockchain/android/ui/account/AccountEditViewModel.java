package piuk.blockchain.android.ui.account;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import info.blockchain.api.Unspent;
import info.blockchain.util.FeeUtil;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SweepBundle;
import info.blockchain.wallet.payment.data.UnspentOutputs;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.payload.PayloadBridge;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.ui.send.SendModel;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.ui.zxing.Contents;
import piuk.blockchain.android.ui.zxing.encode.QRCodeEncoder;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class AccountEditViewModel extends BaseViewModel {

    private Context context;
    private DataListener dataListener;

    @Inject protected PayloadManager payloadManager;
    @Inject protected PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;

    private Account account = null;
    private LegacyAddress legacyAddress = null;
    private int accountIndex;

    public AccountEditModel accountModel;
    private String secondPassword;

    public interface DataListener {
        void onPromptAccountLabel();

        void onConnectivityLoss();

        void onToast(String errorMessage, String type);

        void onSetResult(int resultCode);

        void onStartScanActivity();

        void onPromptPrivateKey(String message);

        void onPromptArchive(String title, String message);

        void onPromptBIP38Password(String data);

        void onPrivateKeyImportMismatch();

        void onPrivateKeyImportSuccess();

        void onShowXpubSharingWarning();

        void onShowAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString);

        void onShowPaymentDetails(PaymentConfirmationDetails details, PendingTransaction pendingTransaction);

        void onShowTransactionSuccess();

        void onShowProgressDialog(String title, String message);

        void onDismissProgressDialog();
    }

    public AccountEditViewModel(AccountEditModel accountModel, Context context, DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.context = context;
        this.dataListener = dataListener;

        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        this.accountModel = accountModel;
    }

    @SuppressWarnings("unused")
    public void setAccountModel(AccountEditModel accountModel) {
        this.accountModel = accountModel;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    @Override
    public void destroy() {
        super.destroy();
        context = null;
        dataListener = null;
    }

    public void setDataFromIntent(Intent intent) {
        int accountIndex = intent.getIntExtra("account_index", -1);
        int addressIndex = intent.getIntExtra("address_index", -1);

        if (accountIndex >= 0) {

            //V3
            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();

            //Remove "All"
            List<Account> accountClone = new ArrayList<>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            this.account = accountClone.get(accountIndex);

            accountModel.setLabel(account.getLabel());
            accountModel.setLabelHeader(context.getString(R.string.name));
            accountModel.setScanPrivateKeyVisibility(View.GONE);
            accountModel.setXpubDescriptionVisibility(View.VISIBLE);
            accountModel.setXpubText(context.getString(R.string.extended_public_key));
            accountModel.setTransferFundsVisibility(View.GONE);
            setArchive(account.isArchived());
            setDefault(isDefault(account));

        } else if (addressIndex >= 0) {

            //V2
            ImportedAccount iAccount = null;
            if (payloadManager.getPayload().getLegacyAddresses().size() > 0) {
                iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            }

            if (iAccount != null) {

                List<LegacyAddress> legacy = iAccount.getLegacyAddresses();
                legacyAddress = legacy.get(addressIndex);


                accountModel.setLabel(legacyAddress.getLabel());
                accountModel.setLabelHeader(context.getString(R.string.name));
                accountModel.setXpubDescriptionVisibility(View.GONE);
                accountModel.setXpubText(context.getString(R.string.address));
                accountModel.setDefaultAccountVisibility(View.GONE);//No default for V2
                setArchive(legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS);

                if (legacyAddress.isWatchOnly()) {
                    accountModel.setScanPrivateKeyVisibility(View.VISIBLE);
                } else {
                    accountModel.setScanPrivateKeyVisibility(View.GONE);
                }

                if (payloadManager.getPayload().isUpgraded()) {
                    long balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
                    //Subtract fee
                    long balanceAfterFee = (balance - FeeUtil.AVERAGE_ABSOLUTE_FEE.longValue());

                    if (balanceAfterFee > SendCoins.bDust.longValue() && !legacyAddress.isWatchOnly()) {
                        accountModel.setTransferFundsVisibility(View.VISIBLE);
                    } else {
                        //No need to show 'transfer' if funds are less than dust amount
                        accountModel.setTransferFundsVisibility(View.GONE);
                    }
                } else {
                    //No transfer option for V2
                    accountModel.setTransferFundsVisibility(View.GONE);
                }
            }
        }
    }

    private void setDefault(boolean isDefault) {
        if (isDefault) {
            accountModel.setDefaultAccountVisibility(View.GONE);
            accountModel.setArchiveAlpha(0.5f);
            accountModel.setArchiveText(context.getString(R.string.default_account_description));
            accountModel.setArchiveClickable(false);
        } else {
            accountModel.setDefaultAccountVisibility(View.VISIBLE);
            accountModel.setDefaultText(context.getString(R.string.make_default));
            accountModel.setDefaultTextColor(context.getResources().getColor(R.color.blockchain_blue));
        }
    }

    private boolean isDefault(Account account) {

        //TODO account.getRealIdx() always returns -1
//        if(account.getRealIdx() == payloadManager.get().getHdWallet().getDefaultIndex())

        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();

        int accountIndex = 0;
        for (Account acc : accounts) {

            if (acc.getXpub().equals(account.getXpub())) {
                this.accountIndex = accountIndex;//sets this account index

                if (accountIndex == defaultIndex) {//this is current default already
                    return true;
                }
            }

            accountIndex++;
        }
        return false;
    }

    private void setArchive(boolean isArchived) {

        if (isArchived) {
            accountModel.setArchiveHeader(context.getString(R.string.unarchive));
            accountModel.setArchiveText(context.getString(R.string.archived_description));
            accountModel.setArchiveAlpha(1.0f);
            accountModel.setArchiveVisibility(View.VISIBLE);
            accountModel.setArchiveClickable(true);

            accountModel.setLabelAlpha(0.5f);
            accountModel.setLabelClickable(false);
            accountModel.setXpubAlpha(0.5f);
            accountModel.setXpubClickable(false);
            accountModel.setXprivAlpha(0.5f);
            accountModel.setXprivClickable(false);
            accountModel.setDefaultAlpha(0.5f);
            accountModel.setDefaultClickable(false);
            accountModel.setTransferFundsAlpha(0.5f);
            accountModel.setTransferFundsClickable(false);
        } else {

            //Don't allow archiving of default account
            if (isArchivable()) {
                accountModel.setArchiveAlpha(1.0f);
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveText(context.getString(R.string.not_archived_description));
                accountModel.setArchiveClickable(true);
            } else {
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveAlpha(0.5f);
                accountModel.setArchiveText(context.getString(R.string.default_account_description));
                accountModel.setArchiveClickable(false);
            }

            accountModel.setArchiveHeader(context.getString(R.string.archive));

            accountModel.setLabelAlpha(1.0f);
            accountModel.setLabelClickable(true);
            accountModel.setXpubAlpha(1.0f);
            accountModel.setXpubClickable(true);
            accountModel.setXprivAlpha(1.0f);
            accountModel.setXprivClickable(true);
            accountModel.setDefaultAlpha(1.0f);
            accountModel.setDefaultClickable(true);
            accountModel.setTransferFundsAlpha(1.0f);
            accountModel.setTransferFundsClickable(true);
        }
    }

    private boolean isArchivable() {

        if (payloadManager.getPayload().isUpgraded()) {
            //V3 - can't archive default account
            int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
            Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);

            if (defaultAccount == account)
                return false;
        } else {
            //V2 - must have a single unarchived address
            List<LegacyAddress> allActiveLegacyAddresses = payloadManager.getPayload().getActiveLegacyAddresses();
            return (allActiveLegacyAddresses.size() > 1);
        }

        return true;
    }

    public void onClickTransferFunds(View view) {

        new SecondPasswordHandler(context).validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                buildTransaction();
            }

            @Override
            public void onSecondPasswordValidated(String validateSecondPassword) {
                setSecondPassword(validateSecondPassword);
                buildTransaction();
            }
        });
    }

    private void buildTransaction() {

        new AsyncTask<Void, Void, PendingTransaction>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
            }

            @Override
            protected PendingTransaction doInBackground(Void... voids) {

                PendingTransaction pendingTransaction = null;
                try {
                    pendingTransaction = getPendingTransaction();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return pendingTransaction;
            }

            @Override
            protected void onPostExecute(PendingTransaction pendingTransaction) {
                super.onPostExecute(pendingTransaction);

                dataListener.onDismissProgressDialog();

                if (pendingTransaction != null && pendingTransaction.bigIntAmount.compareTo(BigInteger.ZERO) == 1) {
                    PaymentConfirmationDetails details = getTransactionDetailsForDisplay(pendingTransaction);
                    dataListener.onShowPaymentDetails(details, pendingTransaction);
                } else {
                    dataListener.onToast(context.getString(R.string.insufficient_funds), ToastCustom.TYPE_ERROR);
                }
            }

        }.execute();
    }

    private PaymentConfirmationDetails getTransactionDetailsForDisplay(PendingTransaction pendingTransaction) {

        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        details.fromLabel = pendingTransaction.sendingObject.label;
        if (pendingTransaction.receivingObject != null
                && pendingTransaction.receivingObject.label != null
                && !pendingTransaction.receivingObject.label.isEmpty()) {
            details.toLabel = pendingTransaction.receivingObject.label;
        } else {
            details.toLabel = pendingTransaction.receivingAddress;
        }

        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = ExchangeRateFactory.getInstance().getLastPrice(fiatUnit);

        details.btcAmount = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntAmount.longValue());
        details.btcFee = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntFee.longValue());
        details.btcSuggestedFee = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntFee.longValue());
        details.btcUnit = btcUnit;
        details.fiatUnit = fiatUnit;
        details.btcTotal = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).longValue());

        details.fiatFee = (monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (pendingTransaction.bigIntFee.doubleValue() / 1e8)));

        details.fiatAmount = (monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (pendingTransaction.bigIntAmount.doubleValue() / 1e8)));

        BigInteger totalFiat = (pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee));
        details.fiatTotal = (monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (totalFiat.doubleValue() / 1e8)));

        details.isSurge = false;
        details.isLargeTransaction = isLargeTransaction(pendingTransaction);
        details.hasConsumedAmounts = pendingTransaction.unspentOutputBundle.getConsumedAmount().compareTo(BigInteger.ZERO) == 1;

        return details;
    }

    public boolean isLargeTransaction(PendingTransaction pendingTransaction) {

        int txSize = FeeUtil.estimatedSize(pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(), 2);//assume change
        double relativeFee = pendingTransaction.bigIntFee.doubleValue() / pendingTransaction.bigIntAmount.doubleValue() * 100.0;

        if (pendingTransaction.bigIntFee.longValue() > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE) {

            return true;
        } else {
            return false;
        }
    }

    private PendingTransaction getPendingTransaction() throws Exception {

        JSONObject unspentResponse = new Unspent().getUnspentOutputs(legacyAddress.getAddress());

        BigInteger suggestedFeePerKb = DynamicFeeCache.getInstance().getSuggestedFee().defaultFeePerKb;

        Payment payment = new Payment();
        UnspentOutputs coins = payment.getCoins(unspentResponse);
        SweepBundle sweepBundle = payment.getSweepBundle(coins, suggestedFeePerKb);

        PendingTransaction pendingTransaction = new PendingTransaction();
        pendingTransaction.sendingObject = new ItemAccount(legacyAddress.getLabel(), sweepBundle.getSweepAmount().toString(), "", legacyAddress);

        //To default account
        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);
        pendingTransaction.receivingObject = new ItemAccount(defaultAccount.getLabel(), "", "", defaultAccount);
        pendingTransaction.receivingAddress = payloadManager.getNextReceiveAddress(defaultIndex);

        pendingTransaction.unspentOutputBundle = payment.getSpendableCoins(coins, sweepBundle.getSweepAmount(), suggestedFeePerKb);
        pendingTransaction.bigIntAmount = sweepBundle.getSweepAmount();
        pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();

        return pendingTransaction;
    }

    public void submitPayment(AlertDialog alertDialog, PendingTransaction pendingTransaction) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dataListener.onDismissProgressDialog();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                try {

                    LegacyAddress legacyAddress = ((LegacyAddress) pendingTransaction.sendingObject.accountObject);
                    String changeAddress = legacyAddress.getAddress();

                    List<ECKey> keys = new ArrayList<ECKey>();
                    if (payloadManager.getPayload().isDoubleEncrypted()) {
                        ECKey walletKey = legacyAddress.getECKey(new CharSequenceX(secondPassword));
                        keys.add(walletKey);
                    } else {
                        ECKey walletKey = legacyAddress.getECKey();
                        keys.add(walletKey);
                    }

                    new Payment().submitPayment(pendingTransaction.unspentOutputBundle,
                            keys,
                            pendingTransaction.receivingAddress,
                            changeAddress,
                            pendingTransaction.bigIntFee,
                            pendingTransaction.bigIntAmount,
                            new Payment.SubmitPaymentListener() {
                                @Override
                                public void onSuccess(String s) {

                                    legacyAddress.setTag(PayloadManager.ARCHIVED_ADDRESS);
                                    setArchive(true);

                                    if (alertDialog != null && alertDialog.isShowing())
                                        alertDialog.dismiss();
                                    dataListener.onShowTransactionSuccess();

                                    //Update v2 balance immediately after spend - until refresh from server
                                    MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue()));
                                    PayloadBridge.getInstance().remoteSaveThread(null);

                                    accountModel.setTransferFundsVisibility(View.GONE);
                                    dataListener.onSetResult(Activity.RESULT_OK);

                                }

                                @Override
                                public void onFail(String s) {
                                    dataListener.onToast(context.getResources().getString(R.string.send_failed), ToastCustom.TYPE_ERROR);
                                }
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                    dataListener.onToast(context.getString(R.string.transaction_failed), ToastCustom.TYPE_ERROR);
                }

                return null;
            }

        }.execute();

    }

    public void updateAccountLabel(String newLabel) {

        if (!ConnectivityStatus.hasConnectivity(context)) {
            dataListener.onConnectivityLoss();
        } else {

            newLabel = newLabel.trim();

            if (newLabel != null && newLabel.length() > 0) {

                final String finalNewLabel = newLabel;
                new AsyncTask<String, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        dataListener.onDismissProgressDialog();
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

                        if (payloadManager.savePayloadToServer()) {
                            dataListener.onSetResult(Activity.RESULT_OK);
                            accountModel.setLabel(finalNewLabel);
                        } else {
                            //Remote save not successful - revert
                            if (account != null) {
                                account.setLabel(revertLabel);
                            } else {
                                legacyAddress.setLabel(revertLabel);
                            }
                            accountModel.setLabel(revertLabel);
                            dataListener.onToast(context.getString(R.string.remote_save_ko), ToastCustom.TYPE_ERROR);
                        }
                        return null;
                    }
                }.execute(newLabel);


            } else {
                dataListener.onToast(context.getResources().getString(R.string.label_cant_be_empty), ToastCustom.TYPE_ERROR);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onClickChangeLabel(View view) {
        dataListener.onPromptAccountLabel();
    }

    @SuppressWarnings("unused")
    public void onClickDefault(View view) {
        new AsyncTask<String, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dataListener.onDismissProgressDialog();
            }

            @Override
            protected Void doInBackground(final String... params) {

                final int revertDefault = payloadManager.getPayload().getHdWallet().getDefaultIndex();
                payloadManager.getPayload().getHdWallet().setDefaultIndex(accountIndex);

                if (payloadManager.savePayloadToServer()) {
                    setDefault(isDefault(account));
                    dataListener.onSetResult(Activity.RESULT_OK);
                } else {
                    //Remote save not successful - revert
                    payloadManager.getPayload().getHdWallet().setDefaultIndex(revertDefault);
                }
                return null;
            }
        }.execute();
    }

    public void onClickScanXpriv(View view) {
        if (payloadManager.getPayload().isDoubleEncrypted()) {
            dataListener.onPromptPrivateKey(String.format(context.getString(R.string.watch_only_spend_instructionss), legacyAddress.getAddress()));
        } else {
            dataListener.onStartScanActivity();
        }
    }

    @SuppressWarnings("unused")
    public void onClickShowXpub(View view) {
        if (account != null) {
            dataListener.onShowXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    @SuppressWarnings("unused")
    public void onClickArchive(View view) {
        String title = context.getResources().getString(R.string.archive);
        String subTitle = context.getResources().getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived()) || (legacyAddress != null && legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)) {
            title = context.getResources().getString(R.string.unarchive);
            subTitle = context.getResources().getString(R.string.unarchive_are_you_sure);
        }

        dataListener.onPromptArchive(title, subTitle);
    }

    private boolean toggleArchived() {

        if (account != null) {
            account.setArchived(!account.isArchived());

            return account.isArchived();

        } else {
            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS) {
                legacyAddress.setTag(PayloadManager.NORMAL_ADDRESS);
                return false;
            } else {
                legacyAddress.setTag(PayloadManager.ARCHIVED_ADDRESS);
                return true;
            }
        }
    }

    private void importNonBIP38Address(final String format, final String data) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dataListener.onDismissProgressDialog();
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
                        } else {
                            importAddressPrivateKey(key, legacyAddress, true);
                        }

                    } else {
                        dataListener.onToast(context.getString(R.string.invalid_private_key), ToastCustom.TYPE_ERROR);
                    }

                } catch (Exception e) {
                    dataListener.onToast(context.getString(R.string.no_private_key), ToastCustom.TYPE_ERROR);
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }

    private void importAddressPrivateKey(ECKey key, LegacyAddress address, boolean matchesIntendedAddress) {
        setLegacyAddressKey(key, address, false);

        if (payloadManager.savePayloadToServer()) {

            dataListener.onSetResult(Activity.RESULT_OK);
            accountModel.setScanPrivateKeyVisibility(View.GONE);

            new Thread(() -> {
                Looper.prepare();
                if (matchesIntendedAddress) {
                    dataListener.onPrivateKeyImportSuccess();
                } else {
                    dataListener.onPrivateKeyImportMismatch();
                }
                Looper.loop();

            }).start();
        }
    }

    private void setLegacyAddressKey(ECKey key, LegacyAddress address, boolean watchOnly) {
        // If double encrypted, save encrypted in payload
        if (!payloadManager.getPayload().isDoubleEncrypted()) {
            address.setEncryptedKey(key.getPrivKeyBytes());
            address.setWatchOnly(watchOnly);
        } else {
            String encryptedKey = Base58.encode(key.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(
                    encryptedKey,
                    payloadManager.getPayload().getSharedKey(),
                    secondPassword,
                    payloadManager.getPayload().getOptions().getIterations());
            address.setEncryptedKey(encrypted2);
            address.setWatchOnly(watchOnly);
        }
    }

    private void importUnmatchedPrivateKey(ECKey key) {
        if (payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
            // Wallet contains address associated with this private key, find & save it with scanned key
            String foundAddressString = key.toAddress(MainNetParams.get()).toString();
            for (LegacyAddress legacyAddress : payloadManager.getPayload().getLegacyAddresses()) {
                if (legacyAddress.getAddress().equals(foundAddressString)) {
                    importAddressPrivateKey(key, legacyAddress, false);
                }
            }
        } else {
            // Create new address and store
            final LegacyAddress legacyAddress = new LegacyAddress(
                    null,
                    System.currentTimeMillis() / 1000L,
                    key.toAddress(MainNetParams.get()).toString(),
                    "",
                    0L,
                    "android",
                    BuildConfig.VERSION_NAME);

            setLegacyAddressKey(key, legacyAddress, true);
            remoteSaveUnmatchedPrivateKey(legacyAddress);

            new Thread(() -> {
                Looper.prepare();
                dataListener.onPrivateKeyImportMismatch();
                Looper.loop();

            }).start();
        }
    }

    private void remoteSaveUnmatchedPrivateKey(final LegacyAddress legacyAddress) {

        Payload updatedPayload = payloadManager.getPayload();
        List<LegacyAddress> updatedLegacyAddresses = updatedPayload.getLegacyAddresses();
        updatedLegacyAddresses.add(legacyAddress);
        updatedPayload.setLegacyAddresses(updatedLegacyAddresses);
        payloadManager.setPayload(updatedPayload);

        if (payloadManager.savePayloadToServer()) {

            List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStrings();
            try {
                MultiAddrFactory.getInstance().refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Subscribe to new address only if successfully created
            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
            intent.putExtra("address", legacyAddress.getAddress());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            dataListener.onSetResult(Activity.RESULT_OK);
        } else {
            dataListener.onToast(context.getString(R.string.remote_save_ko), ToastCustom.TYPE_ERROR);
        }
    }

    public void showAddressDetails() {

        String heading = null;
        String note = null;
        String copy = null;
        String qrString = null;
        Bitmap bitmap = null;

        if (account != null) {

            heading = context.getString(R.string.extended_public_key);
            note = context.getString(R.string.scan_this_code);
            copy = context.getString(R.string.copy_xpub);
            qrString = account.getXpub();

        } else if (legacyAddress != null) {

            heading = context.getString(R.string.address);
            note = legacyAddress.getAddress();
            copy = context.getString(R.string.copy_address);
            qrString = legacyAddress.getAddress();
        }

        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        dataListener.onShowAddressDetails(heading, note, copy, bitmap, qrString);
    }

    public void handleIncomingScanIntent(Intent data) {
        String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);

        try {
            String format = PrivateKeyFactory.getInstance().getFormat(scanData);
            if (format != null) {
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    importNonBIP38Address(format, scanData);
                } else {
                    dataListener.onPromptBIP38Password(scanData);
                }
            } else {
                dataListener.onToast(context.getString(R.string.privkey_error), ToastCustom.TYPE_ERROR);
            }

        } catch (Exception e) {
            dataListener.onToast(context.getString(R.string.scan_not_recognized), ToastCustom.TYPE_ERROR);
            Log.e(AccountEditViewModel.class.getSimpleName(), "handleIncomingScanIntent: ", e);
        }
    }

    public void setSecondPassword(String secondPassword) {
        this.secondPassword = secondPassword;
    }

    public void archiveAccount() {
        if (!ConnectivityStatus.hasConnectivity(context)) {
            dataListener.onConnectivityLoss();
        } else {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    dataListener.onDismissProgressDialog();
                }

                @Override
                protected Void doInBackground(final Void... params) {

                    boolean isArchived = toggleArchived();

                    if (payloadManager.savePayloadToServer()) {

                        try {
                            payloadManager.updateBalancesAndTransactions();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        setArchive(isArchived);
                        dataListener.onSetResult(Activity.RESULT_OK);
                    }
                    return null;
                }
            }.execute();
        }
    }

    public void importBIP38Address(final String data, final String pw) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dataListener.onShowProgressDialog(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.please_wait));
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dataListener.onDismissProgressDialog();
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
                        } else {
                            importAddressPrivateKey(key, legacyAddress, true);
                        }

                    } else {
                        dataListener.onToast(context.getString(R.string.invalid_private_key), ToastCustom.TYPE_ERROR);
                    }
                } catch (Exception e) {
                    dataListener.onToast(context.getString(R.string.bip38_error), ToastCustom.TYPE_ERROR);
                }

                return null;
            }
        }.execute();
    }
}
