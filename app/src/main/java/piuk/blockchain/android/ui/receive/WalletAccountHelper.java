package piuk.blockchain.android.ui.receive;

import android.support.annotation.NonNull;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.AddressBookEntry;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

public class WalletAccountHelper {

    private final PayloadManager payloadManager;
    private final StringUtils stringUtils;
    private AddressBalanceHelper addressBalanceHelper;
    private double btcExchangeRate;
    private String fiatUnit;
    private String btcUnit;

    public WalletAccountHelper(PayloadManager payloadManager, PrefsUtil prefsUtil, StringUtils stringUtils, ExchangeRateFactory exchangeRateFactory) {
        this.payloadManager = payloadManager;
        this.stringUtils = stringUtils;
        int btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        MonetaryUtil monetaryUtil = new MonetaryUtil(btcUnit);
        this.btcUnit = monetaryUtil.getBTCUnit(btcUnit);
        fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btcExchangeRate = exchangeRateFactory.getLastPrice(fiatUnit);

        addressBalanceHelper = new AddressBalanceHelper(monetaryUtil);
    }

    @NonNull
    public List<ItemAccount> getAccountItems(boolean isBtc) {

        List<ItemAccount> accountList = new ArrayList<>();

        // V3
        accountList.addAll(getHdAccounts(isBtc));

        // V2
        List<LegacyAddress> legacyAddresses = payloadManager.getPayload().getLegacyAddressList();
        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == LegacyAddress.ARCHIVED_ADDRESS)
                // Skip archived account
                continue;

            // If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel();
            if (labelOrAddress == null || labelOrAddress.trim().isEmpty()) {
                labelOrAddress = legacyAddress.getAddress();
            }

            // Watch-only tag - we'll ask for xpriv scan when spending from
            String tag = null;
            if (legacyAddress.isWatchOnly()) {
                tag = stringUtils.getString(R.string.watch_only);
            }

            accountList.add(new ItemAccount(
                    labelOrAddress,
                    addressBalanceHelper.getAddressBalance(legacyAddress, isBtc, btcExchangeRate, fiatUnit, btcUnit),
                    tag,
                    legacyAddress));

        }

        return accountList;
    }

    @NonNull
    public List<ItemAccount> getHdAccounts(boolean isBtc) {
        List<ItemAccount> accountArrayList = new ArrayList<>();
        if (payloadManager.getPayload().isUpgraded()) {

            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
            for (Account account : accounts) {

                if (account.isArchived())
                    // Skip archived account
                    continue;

                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(account.getXpub())) {
                    accountArrayList.add(new ItemAccount(
                            account.getLabel(),
                            addressBalanceHelper.getAccountBalance(account, isBtc, btcExchangeRate, fiatUnit, btcUnit),
                            null,
                            account));
                }
            }
        }

        return accountArrayList;
    }

    @NonNull
    public List<ItemAccount> getAddressBookEntries() {
        List<ItemAccount> itemAccountList = new ArrayList<>();

        List<AddressBookEntry> addressBookEntries = payloadManager.getPayload().getAddressBookEntryList();
        for (AddressBookEntry addressBookEntry : addressBookEntries) {

            // If address has no label, we'll display address
            String labelOrAddress = addressBookEntry.getLabel() == null || addressBookEntry.getLabel().length() == 0 ? addressBookEntry.getAddress() : addressBookEntry.getLabel();

            itemAccountList.add(new ItemAccount(labelOrAddress, "", stringUtils.getString(R.string.address_book_label), addressBookEntry));
        }

        return itemAccountList;
    }
}
