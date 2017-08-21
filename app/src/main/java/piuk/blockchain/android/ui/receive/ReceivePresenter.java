package piuk.blockchain.android.ui.receive;

import com.google.common.collect.HashBiMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.FileProvider;
import android.util.Pair;
import android.util.SparseIntArray;
import android.webkit.MimeTypeMap;

import com.crashlytics.android.answers.ShareEvent;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.answers.Logging;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.BitcoinLinkGenerator;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;
import timber.log.Timber;

public class ReceivePresenter extends BasePresenter<ReceiveView> {

    @VisibleForTesting static final String KEY_WARN_WATCH_ONLY_SPEND = "warn_watch_only_spend";
    private static final int DIMENSION_QR_CODE = 600;

    private ReceiveCurrencyHelper currencyHelper;

    private AppUtil appUtil;
    private PrefsUtil prefsUtil;
    private StringUtils stringUtils;
    private QrCodeDataManager qrCodeDataManager;
    private WalletAccountHelper walletAccountHelper;
    private SSLVerifyUtil sslVerifyUtil;
    private Context applicationContext;
    private PayloadDataManager payloadDataManager;
    private ExchangeRateFactory exchangeRateFactory;
    private MonetaryUtil monetaryUtil;
    @VisibleForTesting HashBiMap<Integer, Object> accountMap;
    @VisibleForTesting SparseIntArray spinnerIndexMap;

    private String selectedContactId = null;

    @Inject
    ReceivePresenter(
            AppUtil appUtil,
            PrefsUtil prefsUtil,
            StringUtils stringUtils,
            QrCodeDataManager qrCodeDataManager,
            WalletAccountHelper walletAccountHelper,
            SSLVerifyUtil sslVerifyUtil,
            Context applicationContext,
            PayloadDataManager payloadDataManager,
            ExchangeRateFactory exchangeRateFactory) {

        this.appUtil = appUtil;
        this.prefsUtil = prefsUtil;
        this.stringUtils = stringUtils;
        this.qrCodeDataManager = qrCodeDataManager;
        this.walletAccountHelper = walletAccountHelper;
        this.sslVerifyUtil = sslVerifyUtil;
        this.applicationContext = applicationContext;
        this.payloadDataManager = payloadDataManager;
        this.exchangeRateFactory = exchangeRateFactory;

        int btcUnitType = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        monetaryUtil = new MonetaryUtil(btcUnitType);
        currencyHelper = new ReceiveCurrencyHelper(monetaryUtil, Locale.getDefault(), prefsUtil, exchangeRateFactory);

        accountMap = HashBiMap.create();
        spinnerIndexMap = new SparseIntArray();
    }

    @Override
    public void onViewReady() {
        sslVerifyUtil.validateSSL();

        if(prefsUtil.getValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, false)){
            getView().hideContactsIntroduction();
        } else {
            getView().showContactsIntroduction();
        }

        updateAccountList();
    }

    void onSendToContactClicked() {
        getView().startContactSelectionActivity();
    }

    boolean isValidAmount(String btcAmount) {
        long amountLong = currencyHelper.getLongAmount(btcAmount);
        return amountLong > 0;
    }

    @NonNull
    List<ItemAccount> getReceiveToList() {
        ArrayList<ItemAccount> itemAccounts = new ArrayList<>();
        itemAccounts.addAll(walletAccountHelper.getAccountItems(true));
        itemAccounts.addAll(walletAccountHelper.getAddressBookEntries());
        return itemAccounts;
    }

    @NonNull
    ReceiveCurrencyHelper getCurrencyHelper() {
        return currencyHelper;
    }

    @NonNull
    PrefsUtil getPrefsUtil() {
        return prefsUtil;
    }

    // TODO: 06/02/2017 This is not a nice way of doing things. We need to refactor this stuff
    // into a Map of some description and start passing around HashCodes maybe.
    int getObjectPosition(Object object) {
        for (Object item : accountMap.values()) {
            if (object instanceof Account && item instanceof Account) {
                if (((Account) object).getXpub().equals(((Account) item).getXpub())) {
                    return accountMap.inverse().get(item);
                }
            } else if (object instanceof LegacyAddress && item instanceof LegacyAddress) {
                if (((LegacyAddress) object).getAddress().equals(((LegacyAddress) item).getAddress())) {
                    return accountMap.inverse().get(item);
                }
            }
        }
        return getDefaultAccountPosition();
    }

    int getCorrectedAccountIndex(int accountIndex) {
        // Filter accounts by active
        List<Account> activeAccounts = new ArrayList<>();
        List<Account> accounts = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().indexOf(activeAccounts.get(accountIndex));
    }

    void updateAccountList() {
        accountMap.clear();
        spinnerIndexMap.clear();
        int spinnerIndex = 0;
        // V3
        List<Account> accounts = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts();
        int accountIndex = 0;
        for (Account item : accounts) {
            spinnerIndexMap.put(spinnerIndex, accountIndex);
            accountIndex++;
            if (item.isArchived())
                // Skip archived account
                continue;

            accountMap.put(spinnerIndex, item);
            spinnerIndex++;
        }

        // Legacy Addresses
        List<LegacyAddress> legacyAddresses = payloadDataManager.getWallet().getLegacyAddressList();
        for (LegacyAddress legacyAddress : legacyAddresses) {
            if (legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS)
                // Skip archived address
                continue;

            accountMap.put(spinnerIndex, legacyAddress);
            spinnerIndex++;
        }

        getView().onAccountDataChanged();
    }

    void generateQrCode(String uri) {
        getView().showQrLoading();
        getCompositeDisposable().clear();
        getCompositeDisposable().add(
                qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                        .subscribe(
                                qrCode -> getView().showQrCode(qrCode),
                                throwable -> getView().showQrCode(null)));
    }

    int getDefaultAccountPosition() {
        return accountMap.inverse().get(getDefaultAccount());
    }

    @Nullable
    Object getAccountItemForPosition(int position) {
        return accountMap.get(position);
    }

    boolean warnWatchOnlySpend() {
        return prefsUtil.getValue(KEY_WARN_WATCH_ONLY_SPEND, true);
    }

    void setWarnWatchOnlySpend(boolean warn) {
        prefsUtil.setValue(KEY_WARN_WATCH_ONLY_SPEND, warn);
    }

    void updateFiatTextField(String bitcoin) {
        if (bitcoin.isEmpty()) bitcoin = "0";
        double btcAmount = currencyHelper.getUndenominatedAmount(currencyHelper.getDoubleAmount(bitcoin));
        double fiatAmount = currencyHelper.getLastPrice() * btcAmount;
        getView().updateFiatTextField(currencyHelper.getFormattedFiatString(fiatAmount));
    }

    void updateBtcTextField(String fiat) {
        if (fiat.isEmpty()) fiat = "0";
        double fiatAmount = currencyHelper.getDoubleAmount(fiat);
        double btcAmount = fiatAmount / currencyHelper.getLastPrice();
        getView().updateBtcTextField(currencyHelper.getFormattedBtcString(btcAmount));
    }

    void getV3ReceiveAddress(Account account) {
        getCompositeDisposable().add(
                payloadDataManager.getNextReceiveAddress(account)
                        .subscribe(
                                address -> getView().updateReceiveAddress(address),
                                throwable -> getView().showToast(stringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR)));
    }

    @Nullable
    List<SendPaymentCodeData> getIntentDataList(String uri) {
        File file = getQrFile();
        FileOutputStream outputStream;
        outputStream = getFileOutputStream(file);

        if (outputStream != null) {
            Bitmap bitmap = getView().getQrBitmap();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);

            try {
                outputStream.close();
            } catch (IOException e) {
                Timber.e(e);
                getView().showToast(e.getMessage(), ToastCustom.TYPE_ERROR);
                return null;
            }

            List<SendPaymentCodeData> dataList = new ArrayList<>();

            PackageManager packageManager = appUtil.getPackageManager();

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setType("application/image");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

            if (getFormattedEmailLink(uri) != null) {
                emailIntent.setData(getFormattedEmailLink(uri));
            } else {
                getView().showToast(stringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR);
                return null;
            }

            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String type = mime.getMimeTypeFromExtension(ext);

            Intent imageIntent = new Intent();
            imageIntent.setAction(Intent.ACTION_SEND);
            imageIntent.setType(type);

            if (AndroidUtils.is23orHigher()) {
                Uri uriForFile = FileProvider.getUriForFile(applicationContext, appUtil.getPackageName() + ".fileProvider", file);
                imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                imageIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            } else {
                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                imageIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            HashMap<String, Pair<ResolveInfo, Intent>> intentHashMap = new HashMap<>();

            List<ResolveInfo> emailInfos = packageManager.queryIntentActivities(emailIntent, 0);
            addResolveInfoToMap(emailIntent, intentHashMap, emailInfos);

            List<ResolveInfo> imageInfos = packageManager.queryIntentActivities(imageIntent, 0);
            addResolveInfoToMap(imageIntent, intentHashMap, imageInfos);

            SendPaymentCodeData d;

            Iterator it = intentHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry mapItem = (Map.Entry) it.next();
                @SuppressWarnings("unchecked") Pair<ResolveInfo, Intent> pair =
                        (Pair<ResolveInfo, Intent>) mapItem.getValue();
                ResolveInfo resolveInfo = pair.first;
                String context = resolveInfo.activityInfo.packageName;
                String packageClassName = resolveInfo.activityInfo.name;
                CharSequence label = resolveInfo.loadLabel(packageManager);
                Drawable icon = resolveInfo.loadIcon(packageManager);

                Intent intent = pair.second;
                intent.setClassName(context, packageClassName);

                d = new SendPaymentCodeData(label.toString(), icon, intent);
                dataList.add(d);

                it.remove();
            }

            Logging.INSTANCE.logShare(new ShareEvent()
                    .putContentName("QR Code + URI"));

            return dataList;

        } else {
            getView().showToast(stringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR);
            return null;
        }
    }

    void setSelectedContactId(String contactId) {
        this.selectedContactId = contactId;
    }

    void clearSelectedContactId() {
        this.selectedContactId = null;
    }

    String getSelectedContactId() {
        return this.selectedContactId;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetWorldReadable")
    private File getQrFile() {
        String strFileName = appUtil.getReceiveQRFilename();
        File file = new File(strFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        file.setReadable(true, false);
        return file;
    }

    @Nullable
    private FileOutputStream getFileOutputStream(File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Timber.e(e);
        }
        return fos;
    }

    @Nullable
    private Uri getFormattedEmailLink(String uri) {
        try {
            BitcoinURI addressUri = new BitcoinURI(uri);
            String amount = addressUri.getAmount() != null ? " " + addressUri.getAmount().toPlainString() : "";
            String address = addressUri.getAddress() != null ? addressUri.getAddress().toString() : stringUtils.getString(R.string.email_request_body_fallback);
            String body = String.format(stringUtils.getString(R.string.email_request_body), amount, address);

            String builder = "mailto:" +
                    "?subject=" +
                    stringUtils.getString(R.string.email_request_subject) +
                    "&body=" +
                    body +
                    '\n' +
                    '\n' +
                    BitcoinLinkGenerator.getLink(addressUri);

            return Uri.parse(builder);

        } catch (BitcoinURIParseException e) {
            Timber.e(e);
            return null;
        }
    }

    PaymentConfirmationDetails getConfirmationDetails() {
        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        int position = getView().getSelectedAccountPosition();
        details.fromLabel = payloadDataManager.getAccount(position).getLabel();
        details.toLabel = getView().getContactName();

        int btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double exchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        BigInteger satoshis = getSatoshisFromText(getView().getBtcAmount());

        details.btcAmount = getTextFromSatoshis(satoshis.longValue());
        details.btcUnit = monetaryUtil.getBtcUnit(btcUnit);
        details.fiatUnit = fiatUnit;

        details.fiatAmount = (monetaryUtil.getFiatFormat(fiatUnit)
                .format(exchangeRate * (satoshis.doubleValue() / 1e8)));

        details.fiatSymbol = exchangeRateFactory.getSymbol(fiatUnit);

        return details;
    }

    /**
     * Returns btc amount from satoshis.
     *
     * @return btc, mbtc or bits relative to what is set in monetaryUtil
     */
    private String getTextFromSatoshis(long satoshis) {
        String displayAmount = monetaryUtil.getDisplayAmount(satoshis);
        displayAmount = displayAmount.replace(".", getDefaultDecimalSeparator());
        return displayAmount;
    }

    /**
     * Gets device's specified locale decimal separator
     *
     * @return decimal separator
     */
    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    /**
     * Returns amount of satoshis from btc amount. This could be btc, mbtc or bits.
     *
     * @return satoshis
     */
    private BigInteger getSatoshisFromText(String text) {
        if (text == null || text.isEmpty()) return BigInteger.ZERO;

        String amountToSend = stripSeparator(text);

        double amount;
        try {
            amount = Double.parseDouble(amountToSend);
        } catch (NumberFormatException nfe) {
            amount = 0.0;
        }

        long amountL = BigDecimal.valueOf(monetaryUtil.getUndenominatedAmount(amount))
                .multiply(BigDecimal.valueOf(100000000))
                .longValue();
        return BigInteger.valueOf(amountL);
    }

    private String stripSeparator(String text) {
        return text.trim()
                .replace(" ", "")
                .replace(getDefaultDecimalSeparator(), ".");
    }

    private Account getDefaultAccount() {
        return payloadDataManager.getDefaultAccount();
    }

    /**
     * Prevents apps being added to the list twice, as it's confusing for users. Full email Intent
     * takes priority.
     */
    private void addResolveInfoToMap(Intent intent, HashMap<String, Pair<ResolveInfo, Intent>> intentHashMap, List<ResolveInfo> resolveInfo) {
        //noinspection Convert2streamapi
        for (ResolveInfo info : resolveInfo) {
            if (!intentHashMap.containsKey(info.activityInfo.name)) {
                intentHashMap.put(info.activityInfo.name, new Pair<>(info, new Intent(intent)));
            }
        }
    }

    static class SendPaymentCodeData {
        private Drawable logo;
        private String title;
        private Intent intent;

        SendPaymentCodeData(String title, Drawable logo, Intent intent) {
            this.title = title;
            this.logo = logo;
            this.intent = intent;
        }

        public Intent getIntent() {
            return intent;
        }

        public String getTitle() {
            return title;
        }

        public Drawable getLogo() {
            return logo;
        }
    }
}
