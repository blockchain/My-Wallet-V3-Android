package piuk.blockchain.android.ui.account;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.HDWallet;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.payments.SendDataManager;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.ui.send.SendModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.ui.zxing.Contents;
import piuk.blockchain.android.ui.zxing.encode.QRCodeEncoder;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.LabelUtil;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import timber.log.Timber;

@SuppressWarnings("WeakerAccess")
public class AccountEditPresenter extends BasePresenter<AccountEditView> {

    private PrefsUtil prefsUtil;
    private StringUtils stringUtils;
    private PayloadDataManager payloadDataManager;
    private ExchangeRateFactory exchangeRateFactory;
    private SendDataManager sendDataManager;
    private PrivateKeyFactory privateKeyFactory;
    private SwipeToReceiveHelper swipeToReceiveHelper;
    private DynamicFeeCache dynamicFeeCache;

    // Visible for data binding
    public AccountEditModel accountModel;

    @VisibleForTesting LegacyAddress legacyAddress;
    @VisibleForTesting Account account;
    @VisibleForTesting String secondPassword;
    @VisibleForTesting PendingTransaction pendingTransaction;
    private MonetaryUtil monetaryUtil;
    private int accountIndex;

    @Inject
    AccountEditPresenter(PrefsUtil prefsUtil,
                         StringUtils stringUtils,
                         PayloadDataManager payloadDataManager,
                         ExchangeRateFactory exchangeRateFactory,
                         SendDataManager sendDataManager,
                         PrivateKeyFactory privateKeyFactory,
                         SwipeToReceiveHelper swipeToReceiveHelper,
                         DynamicFeeCache dynamicFeeCache) {

        this.prefsUtil = prefsUtil;
        this.stringUtils = stringUtils;
        this.payloadDataManager = payloadDataManager;
        this.exchangeRateFactory = exchangeRateFactory;
        this.sendDataManager = sendDataManager;
        this.privateKeyFactory = privateKeyFactory;
        this.swipeToReceiveHelper = swipeToReceiveHelper;
        this.dynamicFeeCache = dynamicFeeCache;
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    public void setAccountModel(AccountEditModel accountModel) {
        this.accountModel = accountModel;
    }

    @Override
    public void onViewReady() {
        Intent intent = getView().getIntent();

        int accountIndex = intent.getIntExtra("account_index", -1);
        int addressIndex = intent.getIntExtra("address_index", -1);

        if (accountIndex >= 0) {
            // V3
            List<Account> accounts = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts();

            List<Account> accountClone = new ArrayList<>(accounts.size());
            accountClone.addAll(accounts);

            account = accountClone.get(accountIndex);
            accountModel.setLabel(account.getLabel());
            accountModel.setLabelHeader(stringUtils.getString(R.string.name));
            accountModel.setScanPrivateKeyVisibility(View.GONE);
            accountModel.setXpubDescriptionVisibility(View.VISIBLE);
            accountModel.setXpubText(stringUtils.getString(R.string.extended_public_key));
            accountModel.setTransferFundsVisibility(View.GONE);
            setArchive(account.isArchived());
            setDefault(isDefault(account));

        } else if (addressIndex >= 0) {
            // V2
            List<LegacyAddress> legacy = payloadDataManager.getWallet().getLegacyAddressList();
            legacyAddress = legacy.get(addressIndex);

            accountModel.setLabel(legacyAddress.getLabel());
            accountModel.setLabelHeader(stringUtils.getString(R.string.name));
            accountModel.setXpubDescriptionVisibility(View.GONE);
            accountModel.setXpubText(stringUtils.getString(R.string.address));
            accountModel.setDefaultAccountVisibility(View.GONE);//No default for V2
            setArchive(legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS);

            if (legacyAddress.isWatchOnly()) {
                accountModel.setScanPrivateKeyVisibility(View.VISIBLE);
                accountModel.setArchiveVisibility(View.GONE);
            } else {
                accountModel.setScanPrivateKeyVisibility(View.GONE);
                accountModel.setArchiveVisibility(View.VISIBLE);
            }

            if (payloadDataManager.getWallet().isUpgraded()) {
                // Subtract fee
                long balanceAfterFee = payloadDataManager.getAddressBalance(
                        legacyAddress.getAddress()).longValue() -
                        sendDataManager.estimatedFee(1, 1,
                                BigInteger.valueOf(dynamicFeeCache.getFeeOptions().getRegularFee() * 1000))
                                .longValue();

                if (balanceAfterFee > Payment.DUST.longValue() && !legacyAddress.isWatchOnly()) {
                    accountModel.setTransferFundsVisibility(View.VISIBLE);
                } else {
                    // No need to show 'transfer' if funds are less than dust amount
                    accountModel.setTransferFundsVisibility(View.GONE);
                }
            } else {
                // No transfer option for V2
                accountModel.setTransferFundsVisibility(View.GONE);
            }
        }
    }

    public boolean areLauncherShortcutsEnabled() {
        return prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true);
    }

    private void setDefault(boolean isDefault) {
        if (isDefault) {
            accountModel.setDefaultAccountVisibility(View.GONE);
            accountModel.setArchiveAlpha(0.5f);
            accountModel.setArchiveText(stringUtils.getString(R.string.default_account_description));
            accountModel.setArchiveClickable(false);
        } else {
            accountModel.setDefaultAccountVisibility(View.VISIBLE);
            accountModel.setDefaultText(stringUtils.getString(R.string.make_default));
            accountModel.setDefaultTextColor(R.color.primary_blue_accent);
        }
    }

    private boolean isDefault(Account account) {
        int defaultIndex = payloadDataManager.getDefaultAccountIndex();
        List<Account> accounts = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts();

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
            accountModel.setArchiveHeader(stringUtils.getString(R.string.unarchive));
            accountModel.setArchiveText(stringUtils.getString(R.string.archived_description));
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
            // Don't allow archiving of default account
            if (isArchivable()) {
                accountModel.setArchiveAlpha(1.0f);
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveText(stringUtils.getString(R.string.not_archived_description));
                accountModel.setArchiveClickable(true);
            } else {
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveAlpha(0.5f);
                accountModel.setArchiveText(stringUtils.getString(R.string.default_account_description));
                accountModel.setArchiveClickable(false);
            }

            accountModel.setArchiveHeader(stringUtils.getString(R.string.archive));

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
        Wallet payload = payloadDataManager.getWallet();

        if (payload.isUpgraded()) {
            //V3 - can't archive default account
            HDWallet hdWallet = payload.getHdWallets().get(0);

            int defaultIndex = hdWallet.getDefaultAccountIdx();
            Account defaultAccount = hdWallet.getAccounts().get(defaultIndex);

            if (defaultAccount == account) return false;
        } else {
            //V2 - must have a single unarchived address
            List<LegacyAddress> allActiveLegacyAddresses = payload.getLegacyAddressList(LegacyAddress.NORMAL_ADDRESS);
            return (allActiveLegacyAddresses.size() > 1);
        }

        return true;
    }

    void onClickTransferFunds() {
        getView().showProgressDialog(R.string.please_wait);

        getCompositeDisposable().add(
                getPendingTransactionForLegacyAddress(legacyAddress)
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .doOnNext(pending -> pendingTransaction = pending)
                        .subscribe(pendingTransaction -> {
                            if (pendingTransaction != null && pendingTransaction.bigIntAmount.compareTo(BigInteger.ZERO) == 1) {
                                PaymentConfirmationDetails details = getTransactionDetailsForDisplay(pendingTransaction);
                                getView().showPaymentDetails(details);
                            } else {
                                getView().showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR);
                            }
                        }, throwable -> getView().showToast(R.string.insufficient_funds, ToastCustom.TYPE_ERROR)));

    }

    boolean transferFundsClickable() {
        return accountModel.getTransferFundsClickable();
    }

    private PaymentConfirmationDetails getTransactionDetailsForDisplay(PendingTransaction pendingTransaction) {
        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        details.fromLabel = pendingTransaction.sendingObject.getLabel();
        if (pendingTransaction.receivingObject != null
                && pendingTransaction.receivingObject.getLabel() != null
                && !pendingTransaction.receivingObject.getLabel().isEmpty()) {
            details.toLabel = pendingTransaction.receivingObject.getLabel();
        } else {
            details.toLabel = pendingTransaction.receivingAddress;
        }

        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        String btcUnit = monetaryUtil.getBtcUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        double exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        details.btcAmount = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntAmount.longValue());
        details.btcFee = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntFee.longValue());
        details.btcSuggestedFee = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntFee.longValue());
        details.btcUnit = btcUnit;
        details.fiatUnit = fiatUnit;
        details.btcTotal = monetaryUtil.getDisplayAmount(pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).longValue());

        details.fiatFee = monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (pendingTransaction.bigIntFee.doubleValue() / 1e8));

        details.fiatAmount = monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (pendingTransaction.bigIntAmount.doubleValue() / 1e8));

        BigInteger totalFiat = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee);
        details.fiatTotal = monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * totalFiat.doubleValue() / 1e8);

        details.fiatSymbol = exchangeRateFactory.getSymbol(fiatUnit);
        details.isLargeTransaction = isLargeTransaction(pendingTransaction);
        details.hasConsumedAmounts = pendingTransaction.unspentOutputBundle.getConsumedAmount().compareTo(BigInteger.ZERO) == 1;

        return details;
    }

    private boolean isLargeTransaction(PendingTransaction pendingTransaction) {
        int txSize = sendDataManager.estimateSize(pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(), 2);//assume change
        double relativeFee = pendingTransaction.bigIntFee.doubleValue() / pendingTransaction.bigIntAmount.doubleValue() * 100.0;

        return pendingTransaction.bigIntFee.longValue() > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE;
    }

    void submitPayment() {
        getView().showProgressDialog(R.string.please_wait);

        LegacyAddress legacyAddress = ((LegacyAddress) pendingTransaction.sendingObject.getAccountObject());
        String changeAddress = legacyAddress.getAddress();

        List<ECKey> keys = new ArrayList<>();

        try {
            ECKey walletKey = payloadDataManager.getAddressECKey(legacyAddress, secondPassword);
            if (walletKey == null) throw new NullPointerException("ECKey was null");
            keys.add(walletKey);
        } catch (Exception e) {
            getView().dismissProgressDialog();
            getView().showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR);
            return;
        }

        getCompositeDisposable().add(
                sendDataManager.submitPayment(pendingTransaction.unspentOutputBundle,
                        keys,
                        pendingTransaction.receivingAddress,
                        changeAddress,
                        pendingTransaction.bigIntFee,
                        pendingTransaction.bigIntAmount)
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(hash -> {
                            legacyAddress.setTag(LegacyAddress.ARCHIVED_ADDRESS);
                            setArchive(true);

                            getView().showTransactionSuccess();

                            // Update V2 balance immediately after spend - until refresh from server
                            long spentAmount = (pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue());

                            if (pendingTransaction.sendingObject.getAccountObject() instanceof Account) {
                                payloadDataManager.subtractAmountFromAddressBalance(
                                        ((Account) pendingTransaction.sendingObject.getAccountObject()).getXpub(), spentAmount);
                            } else {
                                payloadDataManager.subtractAmountFromAddressBalance(
                                        ((LegacyAddress) pendingTransaction.sendingObject.getAccountObject()).getAddress(), spentAmount);
                            }

                            payloadDataManager.syncPayloadWithServer()
                                    .subscribe(new IgnorableDefaultObserver<>());

                            accountModel.setTransferFundsVisibility(View.GONE);
                            getView().setActivityResult(Activity.RESULT_OK);
                        }, throwable -> getView().showToast(R.string.send_failed, ToastCustom.TYPE_ERROR)));
    }

    void updateAccountLabel(String newLabel) {
        newLabel = newLabel.trim();

        if (!newLabel.isEmpty()) {
            String finalNewLabel = newLabel;
            String revertLabel;

            if (LabelUtil.isExistingLabel(payloadDataManager, newLabel)) {
                getView().showToast(R.string.label_name_match, ToastCustom.TYPE_ERROR);
                return;
            }

            if (account != null) {
                revertLabel = account.getLabel();
                account.setLabel(finalNewLabel);
            } else {
                revertLabel = legacyAddress.getLabel();
                legacyAddress.setLabel(finalNewLabel);
            }

            getCompositeDisposable().add(
                    payloadDataManager.syncPayloadWithServer()
                            .doOnSubscribe(ignored -> getView().showProgressDialog(R.string.please_wait))
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(() -> {
                                accountModel.setLabel(finalNewLabel);
                                getView().setActivityResult(Activity.RESULT_OK);
                            }, throwable -> revertLabelAndShowError(revertLabel)));
        } else {
            getView().showToast(R.string.label_cant_be_empty, ToastCustom.TYPE_ERROR);
        }
    }

    private void revertLabelAndShowError(String revertLabel) {
        // Remote save not successful - revert
        if (account != null) {
            account.setLabel(revertLabel);
        } else {
            legacyAddress.setLabel(revertLabel);
        }
        accountModel.setLabel(revertLabel);
        getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    public void onClickChangeLabel(View view) {
        getView().promptAccountLabel(accountModel.getLabel());
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    public void onClickDefault(View view) {
        int revertDefault = payloadDataManager.getDefaultAccountIndex();
        payloadDataManager.getWallet().getHdWallets().get(0).setDefaultAccountIdx(accountIndex);

        getView().showProgressDialog(R.string.please_wait);

        getCompositeDisposable().add(
                payloadDataManager.syncPayloadWithServer()
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(() -> {
                            setDefault(isDefault(account));
                            updateSwipeToReceiveAddresses();
                            getView().updateAppShortcuts();
                            getView().setActivityResult(Activity.RESULT_OK);
                        }, throwable -> revertDefaultAndShowError(revertDefault)));
    }

    private void updateSwipeToReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        getCompositeDisposable().add(
                Completable.fromCallable(() -> {
                    swipeToReceiveHelper.updateAndStoreAddresses();
                    return Void.TYPE;
                }).subscribeOn(Schedulers.computation())
                        .subscribe(() -> {
                            // No-op
                        }, Throwable::printStackTrace));
    }

    private void revertDefaultAndShowError(int revertDefault) {
        // Remote save not successful - revert
        payloadDataManager.getWallet().getHdWallets().get(0).setDefaultAccountIdx(revertDefault);
        getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
    }

    @SuppressWarnings("unused")
    public void onClickScanXpriv(View view) {
        if (payloadDataManager.getWallet().isDoubleEncryption()) {
            getView().promptPrivateKey(String.format(stringUtils.getString(R.string.watch_only_spend_instructionss), legacyAddress.getAddress()));
        } else {
            getView().startScanActivity();
        }
    }

    @SuppressWarnings("unused")
    public void onClickShowXpub(View view) {
        if (account != null) {
            getView().showXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    @SuppressWarnings("unused")
    public void onClickArchive(View view) {
        String title = stringUtils.getString(R.string.archive);
        String subTitle = stringUtils.getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived())
                || (legacyAddress != null && legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS)) {
            title = stringUtils.getString(R.string.unarchive);
            subTitle = stringUtils.getString(R.string.unarchive_are_you_sure);
        }

        getView().promptArchive(title, subTitle);
    }

    private boolean toggleArchived() {
        if (account != null) {
            account.setArchived(!account.isArchived());
            return account.isArchived();
        } else {
            if (legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS) {
                legacyAddress.setTag(LegacyAddress.NORMAL_ADDRESS);
                return false;
            } else {
                legacyAddress.setTag(LegacyAddress.ARCHIVED_ADDRESS);
                return true;
            }
        }
    }

    @VisibleForTesting
    void importAddressPrivateKey(ECKey key, LegacyAddress address, boolean matchesIntendedAddress) throws Exception {
        setLegacyAddressKey(key, address);

        getCompositeDisposable().add(
                payloadDataManager.syncPayloadWithServer()
                        .subscribe(() -> {
                            getView().setActivityResult(Activity.RESULT_OK);
                            accountModel.setScanPrivateKeyVisibility(View.GONE);
                            accountModel.setArchiveVisibility(View.VISIBLE);

                            if (matchesIntendedAddress) {
                                getView().privateKeyImportSuccess();
                            } else {
                                getView().privateKeyImportMismatch();
                            }
                        }, throwable -> getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    private void setLegacyAddressKey(ECKey key, LegacyAddress address) throws Exception {
        // If double encrypted, save encrypted in payload
        if (!payloadDataManager.getWallet().isDoubleEncryption()) {
            address.setPrivateKeyFromBytes(key.getPrivKeyBytes());
        } else {
            String encryptedKey = Base58.encode(key.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.encrypt(
                    encryptedKey,
                    payloadDataManager.getWallet().getSharedKey(),
                    secondPassword,
                    payloadDataManager.getWallet().getOptions().getPbkdf2Iterations());
            address.setPrivateKey(encrypted2);
        }
    }

    void importUnmatchedPrivateKey(ECKey key) throws Exception {
        if (payloadDataManager.getWallet().getLegacyAddressStringList().contains(key.toAddress(MainNetParams.get()).toString())) {
            // Wallet contains address associated with this private key, find & save it with scanned key
            String foundAddressString = key.toAddress(MainNetParams.get()).toString();
            for (LegacyAddress legacyAddress : payloadDataManager.getWallet().getLegacyAddressList()) {
                if (legacyAddress.getAddress().equals(foundAddressString)) {
                    importAddressPrivateKey(key, legacyAddress, false);
                    break;
                }
            }
        } else {
            // Create new address and store
            final LegacyAddress legacyAddress = LegacyAddress.fromECKey(key);

            setLegacyAddressKey(key, legacyAddress);
            remoteSaveUnmatchedPrivateKey(legacyAddress);

            getView().privateKeyImportMismatch();
        }
    }

    void showAddressDetails() {
        String heading = null;
        String note = null;
        String copy = null;
        String qrString = null;
        Bitmap bitmap = null;

        if (account != null) {
            heading = stringUtils.getString(R.string.extended_public_key);
            note = stringUtils.getString(R.string.scan_this_code);
            copy = stringUtils.getString(R.string.copy_xpub);
            qrString = account.getXpub();

        } else if (legacyAddress != null) {
            heading = stringUtils.getString(R.string.address);
            note = legacyAddress.getAddress();
            copy = stringUtils.getString(R.string.copy_address);
            qrString = legacyAddress.getAddress();
        }

        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            Timber.e(e);
        }

        getView().showAddressDetails(heading, note, copy, bitmap, qrString);
    }

    void handleIncomingScanIntent(Intent data) {
        String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);
        String format = privateKeyFactory.getFormat(scanData);
        if (format != null) {
            if (!format.equals(PrivateKeyFactory.BIP38)) {
                importNonBIP38Address(format, scanData);
            } else {
                getView().promptBIP38Password(scanData);
            }
        } else {
            getView().showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR);
        }
    }

    public void setSecondPassword(String secondPassword) {
        this.secondPassword = secondPassword;
    }

    void archiveAccount() {
        getView().showProgressDialog(R.string.please_wait);

        boolean isArchived = toggleArchived();
        getCompositeDisposable().add(
                payloadDataManager.syncPayloadWithServer()
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .subscribe(() -> {
                            payloadDataManager.updateAllTransactions()
                                    .subscribe(new IgnorableDefaultObserver<>());

                            setArchive(isArchived);
                            getView().setActivityResult(Activity.RESULT_OK);
                        }, throwable -> getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    private void importNonBIP38Address(final String format, final String data) {
        getView().showProgressDialog(R.string.please_wait);

        try {
            final ECKey key = privateKeyFactory.getKey(format, data);
            if (key != null && key.hasPrivKey()) {
                final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                if (!legacyAddress.getAddress().equals(keyAddress)) {
                    // Private key does not match this address - warn user but import nevertheless
                    importUnmatchedPrivateKey(key);
                } else {
                    importAddressPrivateKey(key, legacyAddress, true);
                }
            } else {
                getView().showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR);
            }
        } catch (Exception e) {
            getView().showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR);
        }

        getView().dismissProgressDialog();
    }

    void importBIP38Address(final String data, final String pw) {
        getView().showProgressDialog(R.string.please_wait);

        try {
            BIP38PrivateKey bip38 = BIP38PrivateKey.fromBase58(MainNetParams.get(), data);
            final ECKey key = bip38.decrypt(pw);

            if (key != null && key.hasPrivKey()) {
                final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                if (!legacyAddress.getAddress().equals(keyAddress)) {
                    // Private key does not match this address - warn user but import nevertheless
                    importUnmatchedPrivateKey(key);
                } else {
                    importAddressPrivateKey(key, legacyAddress, true);
                }

            } else {
                getView().showToast(R.string.invalid_private_key, ToastCustom.TYPE_ERROR);
            }
        } catch (Exception e) {
            getView().showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR);
            Timber.e(e);
        }

        getView().dismissProgressDialog();
    }

    private void remoteSaveUnmatchedPrivateKey(final LegacyAddress legacyAddress) {
        List<LegacyAddress> addressCopy = new ArrayList<>(payloadDataManager.getLegacyAddresses());
        addressCopy.add(legacyAddress);
        payloadDataManager.getLegacyAddresses().clear();
        payloadDataManager.getLegacyAddresses().addAll(addressCopy);

        getCompositeDisposable().add(
                payloadDataManager.syncPayloadWithServer()
                        .subscribe(() -> {
                            // Subscribe to new address only if successfully created
                            getView().sendBroadcast("address", legacyAddress.getAddress());
                            getView().setActivityResult(Activity.RESULT_OK);
                        }, throwable -> getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)));
    }

    /**
     * Generates a {@link PendingTransaction} object for a given legacy address, where the output is
     * the default account in the user's wallet
     *
     * @param legacyAddress The {@link LegacyAddress} you wish to transfer funds from
     * @return An {@link Observable <PendingTransaction>}
     */
    private Observable<PendingTransaction> getPendingTransactionForLegacyAddress(LegacyAddress legacyAddress) {
        PendingTransaction pendingTransaction = new PendingTransaction();

        return sendDataManager.getUnspentOutputs(legacyAddress.getAddress())
                .flatMap(unspentOutputs -> {
                    BigInteger suggestedFeePerKb =
                            BigInteger.valueOf(dynamicFeeCache.getFeeOptions().getRegularFee() * 1000);

                    Pair<BigInteger, BigInteger> sweepableCoins =
                            sendDataManager.getSweepableCoins(unspentOutputs, suggestedFeePerKb);
                    BigInteger sweepAmount = sweepableCoins.getLeft();

                    // To default account
                    Account defaultAccount = payloadDataManager.getDefaultAccount();
                    pendingTransaction.sendingObject = new ItemAccount(legacyAddress.getLabel(), sweepAmount.toString(), "", sweepAmount.longValue(), legacyAddress, legacyAddress.getAddress());
                    pendingTransaction.receivingObject = new ItemAccount(defaultAccount.getLabel(), "", "", sweepAmount.longValue(), defaultAccount, null);
                    pendingTransaction.unspentOutputBundle = sendDataManager.getSpendableCoins(unspentOutputs, sweepAmount, suggestedFeePerKb);
                    pendingTransaction.bigIntAmount = sweepAmount;
                    pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();

                    return payloadDataManager.getNextReceiveAddress(defaultAccount);
                })
                .map(receivingAddress -> {
                    pendingTransaction.receivingAddress = receivingAddress;
                    return pendingTransaction;
                });
    }

    public PayloadDataManager getPayloadDataManager() {
        return payloadDataManager;
    }
}
