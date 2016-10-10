package piuk.blockchain.android.ui.receive;

import com.google.common.collect.HashBiMap;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ReceiveDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.BitcoinLinkGenerator;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;

public class ReceiveViewModel extends BaseViewModel {

    public static final String TAG = ReceiveViewModel.class.getSimpleName();
    static final String KEY_WARN_WATCH_ONLY_SPEND = "warn_watch_only_spend";
    private static final int DIMENSION_QR_CODE = 600;

    private DataListener mDataListener;
    @Inject PayloadManager mPayloadManager;
    @Inject AppUtil mAppUtil;
    @Inject PrefsUtil mPrefsUtil;
    @Inject StringUtils mStringUtils;
    @Inject ReceiveDataManager mDataManager;
    @Inject WalletAccountHelper mWalletAccountHelper;
    @Inject SSLVerifyUtil mSSLVerifyUtil;
    @VisibleForTesting HashBiMap<Integer, Object> mAccountMap;
    @VisibleForTesting SparseIntArray mSpinnerIndexMap;
    private ReceiveCurrencyHelper mCurrencyHelper;

    public interface DataListener {

        Bitmap getQrBitmap();

        void onSpinnerDataChanged();

        void showQrLoading();

        void showQrCode(@Nullable Bitmap bitmap);

        void showToast(String message, @ToastCustom.ToastType String toastType);

        void updateFiatTextField(String text);

        void updateBtcTextField(String text);

    }

    public ReceiveViewModel(DataListener listener, Locale locale) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;

        int btcUnitType = mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        MonetaryUtil monetaryUtil = new MonetaryUtil(btcUnitType);
        mCurrencyHelper = new ReceiveCurrencyHelper(monetaryUtil, locale);

        mAccountMap = HashBiMap.create();
        mSpinnerIndexMap = new SparseIntArray();
    }

    @Override
    public void onViewReady() {
        mSSLVerifyUtil.validateSSL();
        updateSpinnerList();
    }

    @NonNull
    public List<ItemAccount> getReceiveToList() {
        return new ArrayList<ItemAccount>() {{
            addAll(mWalletAccountHelper.getAccountItems(true));
            addAll(mWalletAccountHelper.getAddressBookEntries());
        }};
    }

    @NonNull
    public ReceiveCurrencyHelper getCurrencyHelper() {
        return mCurrencyHelper;
    }

    public void updateSpinnerList() {
        mAccountMap.clear();
        mSpinnerIndexMap.clear();

        int spinnerIndex = 0;

        if (isUpgraded()) {
            // V3
            List<Account> accounts = mPayloadManager.getPayload().getHdWallet().getAccounts();
            int accountIndex = 0;
            for (Account item : accounts) {

                mSpinnerIndexMap.put(spinnerIndex, accountIndex);
                accountIndex++;

                if (item.isArchived())
                    // Skip archived account
                    continue;

                mAccountMap.put(spinnerIndex, item);
                spinnerIndex++;
            }
        }

        // Legacy Addresses
        List<LegacyAddress> legacyAddresses = mPayloadManager.getPayload().getLegacyAddresses();
        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)
                // Skip archived address
                continue;

            mAccountMap.put(spinnerIndex, legacyAddress);
            spinnerIndex++;
        }

        mDataListener.onSpinnerDataChanged();
    }

    public void generateQrCode(String uri) {
        mDataListener.showQrLoading();
        mCompositeSubscription.clear();
        mCompositeSubscription.add(
                mDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                        .subscribe(qrCode -> {
                            mDataListener.showQrCode(qrCode);
                        }, throwable -> {
                            mDataListener.showQrCode(null);
                        }));
    }

    public int getDefaultSpinnerPosition() {
        if (isUpgraded()) {
            return mAccountMap.inverse().get(getDefaultAccount());
        } else {
            return 0;
        }
    }

    private Account getDefaultAccount() {
        return mPayloadManager.getPayload().getHdWallet().getAccounts().get(
                mPayloadManager.getPayload().getHdWallet().getDefaultIndex());
    }

    @Nullable
    public Object getAccountItemForPosition(int position) {
        return mAccountMap.get(position);
    }

    public boolean isUpgraded() {
        return mPayloadManager.getPayload().isUpgraded();
    }

    public boolean warnWatchOnlySpend() {
        return mPrefsUtil.getValue(KEY_WARN_WATCH_ONLY_SPEND, true);
    }

    public void setWarnWatchOnlySpend(boolean warn) {
        mPrefsUtil.setValue(KEY_WARN_WATCH_ONLY_SPEND, warn);
    }

    public void updateFiatTextField(String bitcoin) {
        if (bitcoin.isEmpty()) bitcoin = "0";
        double btcAmount = mCurrencyHelper.getUndenominatedAmount(mCurrencyHelper.getDoubleAmount(bitcoin));
        double fiatAmount = mCurrencyHelper.getLastPrice() * btcAmount;
        mDataListener.updateFiatTextField(mCurrencyHelper.getFormattedFiatString(fiatAmount));
    }

    public void updateBtcTextField(String fiat) {
        if (fiat.isEmpty()) fiat = "0";
        double fiatAmount = mCurrencyHelper.getDoubleAmount(fiat);
        double btcAmount = fiatAmount / mCurrencyHelper.getLastPrice();
        mDataListener.updateBtcTextField(mCurrencyHelper.getFormattedBtcString(btcAmount));
    }

    @Nullable
    private File getQrFile() {
        String strFileName = mAppUtil.getReceiveQRFilename();
        File file = new File(strFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.e(TAG, "getQrFile: ", e);
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
            Log.e(TAG, "getFileOutputStream: ", e);
        }
        return fos;
    }

    @Nullable
    public String getV3ReceiveAddress(Account account) {
        try {
            int spinnerIndex = mAccountMap.inverse().get(account);
            int accountIndex = mSpinnerIndexMap.get(spinnerIndex);
            return mPayloadManager.getNextReceiveAddress(accountIndex);
        } catch (Exception e) {
            Log.e(TAG, "getV3ReceiveAddress: ", e);
            return null;
        }
    }

    @Nullable
    public List<SendPaymentCodeData> getIntentDataList(String uri) {
        File file = getQrFile();
        FileOutputStream outputStream;
        if (file != null) {
            outputStream = getFileOutputStream(file);

            if (outputStream != null) {
                Bitmap bitmap = mDataListener.getQrBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);

                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "getIntentDataList: ", e);
                    mDataListener.showToast(e.getMessage(), ToastCustom.TYPE_ERROR);
                    return null;
                }

                List<SendPaymentCodeData> dataList = new ArrayList<>();

                PackageManager packageManager = mAppUtil.getPackageManager();

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setType("application/image");
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

                Intent imageIntent = new Intent();
                imageIntent.setAction(Intent.ACTION_SEND);
                imageIntent.setType("image/png");
                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

                if (getFormattedEmailLink(uri) != null) {
                    emailIntent.setData(getFormattedEmailLink(uri));
                } else {
                    mDataListener.showToast(mStringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR);
                    return null;
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
                    Pair<ResolveInfo, Intent> pair = (Pair<ResolveInfo, Intent>) mapItem.getValue();
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

                return dataList;

            } else {
                mDataListener.showToast(mStringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR);
                return null;
            }
        } else {
            mDataListener.showToast(mStringUtils.getString(R.string.unexpected_error), ToastCustom.TYPE_ERROR);
            return null;
        }
    }

    @Nullable
    private Uri getFormattedEmailLink(String uri) {
        try {
            BitcoinURI addressUri = new BitcoinURI(uri);
            String amount = addressUri.getAmount() != null ? " " + addressUri.getAmount().toPlainString() : "";
            String address = addressUri.getAddress() != null ? addressUri.getAddress().toString() : mStringUtils.getString(R.string.email_request_body_fallback);
            String body = String.format(mStringUtils.getString(R.string.email_request_body), amount, address);

            String builder = "mailto:" +
                    "?subject=" +
                    mStringUtils.getString(R.string.email_request_subject) +
                    "&body=" +
                    body +
                    '\n' +
                    '\n' +
                    BitcoinLinkGenerator.getLink(addressUri);

            return Uri.parse(builder);

        } catch (BitcoinURIParseException e) {
            Log.e(TAG, "getFormattedEmailLink: ", e);
            return null;
        }
    }

    /**
     * Prevents apps being added to the list twice, as it's confusing for users. Full email Intent
     * takes priority.
     */
    private void addResolveInfoToMap(Intent intent, HashMap<String, Pair<ResolveInfo, Intent>> intentHashMap, List<ResolveInfo> resolveInfo) {
        for (ResolveInfo info : resolveInfo) {
            if (!intentHashMap.containsKey(info.activityInfo.name)) {
                intentHashMap.put(info.activityInfo.name, new Pair<>(info, new Intent(intent)));
            }
        }
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }

    public class SendPaymentCodeData {
        private Drawable mLogo;
        private String mTitle;
        private Intent mIntent;

        SendPaymentCodeData(String title, Drawable logo, Intent intent) {
            mTitle = title;
            mLogo = logo;
            mIntent = intent;
        }

        public Intent getIntent() {
            return mIntent;
        }

        public String getTitle() {
            return mTitle;
        }

        public Drawable getLogo() {
            return mLogo;
        }
    }
}
