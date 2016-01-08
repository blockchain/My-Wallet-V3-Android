package info.blockchain.wallet.util;

import android.content.Context;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import piuk.blockchain.android.R;

public class AccountsUtil {

    private static Context context = null;
    private static AccountsUtil instance = null;

    private static int currentSpinnerIndex = 0;//The current selected spinner index for balance/receive/send fragments

    //Balance Screen
    private static LinkedHashMap<Integer, Account> balanceAccountMap = null;
    private static LinkedHashMap<Integer, Integer> balanceAccountIndexResolver = null;

    //Send/Receive Screen
    private static LinkedHashMap<Integer, Account> sendReceiveAccountMap = null;
    private static LinkedHashMap<Integer, Integer> sendReceiveAccountIndexResolver = null;
    private static List<String> sendReceiveAccountList = null;//used for spinner
    private static int lastHDIndex = 0;
    private static List<LegacyAddress> legacyAddresses;

    public static AccountsUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            initAccountMaps();
            instance = new AccountsUtil();
        }

        return instance;
    }

    public static void initAccountMaps() {
        initBalanceAccountMap();
        initSendReceiveAccountMap();
    }

    public static void initBalanceAccountMap() {

        balanceAccountMap = new LinkedHashMap<Integer, Account>();
        balanceAccountIndexResolver = new LinkedHashMap<Integer, Integer>();
        int accountIndex = 0;
        int spinnerIndex = 0;

        //Get all accounts
        HDWallet hdWallet = PayloadFactory.getInstance().get().getHdWallet();
        List<Account> allAccounts = new ArrayList<>();
        if (hdWallet != null && hdWallet.getAccounts() != null)
            allAccounts = hdWallet.getAccounts();

        //Get all legacy addresses
        List<LegacyAddress> allLegacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

        //Exclude archived legacy addresses
        //TODO - current we include all address types

        //Add "All Accounts" to map - If wallet contains multiple unarchived accounts or contains legacy address we need to display "All Accounts" at top of accounts spinner
        if (allAccounts != null && allAccounts.size() > 1 || allLegacyAddresses.size() > 0) {

            if (PayloadFactory.getInstance().get().isUpgraded()) {

                //Only V3 will display "All Accounts"
                Account all = new Account();
                all.setLabel(context.getResources().getString(R.string.all_accounts));
                balanceAccountMap.put(-1, all);
                balanceAccountIndexResolver.put(spinnerIndex, -1);
                spinnerIndex++;

            } else if (allLegacyAddresses.size() > 1) {

                //Lame mode or an un-upgraded V3 wallet will display "Total Funds" at top of accounts spinner if wallet contains multiple legacy addresses
                ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.total_funds), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
                balanceAccountMap.put(-1, iAccount);
                balanceAccountIndexResolver.put(spinnerIndex, -1);
                spinnerIndex++;
            }
        }

        //Add accounts to map
        for (Account item : allAccounts) {

            //Exclude archived accounts
            if (!item.isArchived()) {
                if (item.getLabel().length() == 0)
                    item.setLabel("Account: " + accountIndex);//Give unlabeled account a label

                balanceAccountMap.put(accountIndex, item);
                balanceAccountIndexResolver.put(spinnerIndex, accountIndex);
                spinnerIndex++;
            }

            accountIndex++;
        }

        //Add "Imported Addresses" or "Total Funds" to map
        if (PayloadFactory.getInstance().get().isUpgraded()) {

            //Only V3 - Consolidate and add Legacy addresses to "Imported Addresses" at bottom of accounts spinner
            if (balanceAccountMap.size() > 0
                    && !(balanceAccountMap.get(balanceAccountMap.size() - 1) instanceof ImportedAccount)
                    && (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0)) {

                ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
                balanceAccountMap.put(accountIndex, iAccount);
                balanceAccountIndexResolver.put(spinnerIndex, accountIndex);
                spinnerIndex++;
                accountIndex++;
            }
        } else {

            //Lame mode or an un-upgraded V3 wallet we will display legacy addresses individually in accounts spinner
            if (allLegacyAddresses != null) {
                for (int j = 0; j < allLegacyAddresses.size(); j++) {

                    LegacyAddress legacyAddress = allLegacyAddresses.get(j);

                    //If legacy address contains no label we set label to address for display purposes
                    if (legacyAddress.getLabel() == null || legacyAddress.getLabel().length() == 0)
                        legacyAddress.setLabel(legacyAddress.getAddress());

                    //Prefix label with (Watch Only) or (Archived)
                    String labelOrAddress = legacyAddress.getLabel();
                    if (legacyAddress.isWatchOnly()) {
                        labelOrAddress = context.getString(R.string.watch_only_label) + " " + labelOrAddress;
                    } else if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                        labelOrAddress = context.getString(R.string.archived_label) + " " + labelOrAddress;
                    }

                    //Consolidate legacy addresses into instance of Account (Imported Addresses) and add to dropdown list
                    ImportedAccount importedAddresses = new ImportedAccount(legacyAddress.getLabel(), new ArrayList<>(Arrays.asList(legacyAddress)), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress()));
                    importedAddresses.setLabel(labelOrAddress);
                    balanceAccountMap.put(accountIndex, importedAddresses);
                    balanceAccountIndexResolver.put(spinnerIndex, accountIndex);
                    spinnerIndex++;
                    accountIndex++;
                }
            }
        }
    }

    public static void initSendReceiveAccountMap() {

        sendReceiveAccountMap = new LinkedHashMap<Integer, Account>();
        sendReceiveAccountIndexResolver = new LinkedHashMap<Integer, Integer>();
        sendReceiveAccountList = new ArrayList<String>();
        int accountIndex = 0;
        int spinnerIndex = 0;

        if (PayloadFactory.getInstance().get().isUpgraded()) {
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
            for (Account item : accounts) {

                if (item instanceof ImportedAccount) continue;
                if (!item.isArchived()) {

                    if (item.getLabel().length() == 0) item.setLabel("Account: " + accountIndex);

                    sendReceiveAccountMap.put(accountIndex, item);
                    sendReceiveAccountIndexResolver.put(spinnerIndex, accountIndex);
                    spinnerIndex++;
                }

                accountIndex++;
            }

            sendReceiveAccountList = new ArrayList<String>();
            for (Account account : sendReceiveAccountMap.values()) {
                sendReceiveAccountList.add(account.getLabel());
            }
            lastHDIndex = sendReceiveAccountList.size();
        }

        //Add individual legacy addresses
        ImportedAccount iAccount = null;
        if (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
            iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        }
        if (iAccount != null) {
            legacyAddresses = iAccount.getLegacyAddresses();
            for (int j = 0; j < legacyAddresses.size(); j++) {
                final LegacyAddress legacyAddress = legacyAddresses.get(j);
                String labelOrAddress = legacyAddress.getLabel() == null || legacyAddress.getLabel().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();
                if (legacyAddress.isWatchOnly()) {
                    labelOrAddress = context.getString(R.string.watch_only_label) + " " + labelOrAddress;
                } else if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                    labelOrAddress = context.getString(R.string.archived_label) + " " + labelOrAddress;
                }
                sendReceiveAccountList.add(labelOrAddress);
            }
        } else if (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
            sendReceiveAccountList.add((PayloadFactory.getInstance().get().getLegacyAddresses().get(0).getLabel() == null)
                    || (PayloadFactory.getInstance().get().getLegacyAddresses().get(0).getLabel().length() == 0)
                    ? PayloadFactory.getInstance().get().getLegacyAddresses().get(0).getAddress()
                    : PayloadFactory.getInstance().get().getLegacyAddresses().get(0).getLabel());
        }
    }

    public static LinkedHashMap<Integer, Account> getSendReceiveAccountMap() {
        return sendReceiveAccountMap;
    }

    public static List<String> getSendReceiveAccountList() {
        return sendReceiveAccountList;
    }

    public static int getLastHDIndex() {
        return lastHDIndex;
    }

    public static LegacyAddress getLegacyAddress(int position) {
        return legacyAddresses.get(Math.max(position,0));
    }

    public static List<LegacyAddress> getLegacyAddresses() {
        return legacyAddresses;
    }

    public int getCurrentSpinnerIndex() {
        return currentSpinnerIndex;
    }

    public void setCurrentSpinnerIndex(int currentSpinnerIndex) {
        this.currentSpinnerIndex = currentSpinnerIndex;
    }

    public LinkedHashMap<Integer, Account> getBalanceAccountMap() {
        return balanceAccountMap;
    }

    public LinkedHashMap<Integer, Integer> getBalanceAccountIndexResolver() {
        return balanceAccountIndexResolver;
    }

    public LinkedHashMap<Integer, Integer> getSendReceiveAccountIndexResolver() {
        return sendReceiveAccountIndexResolver;
    }
}