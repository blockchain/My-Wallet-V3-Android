package piuk.blockchain.android.util;

import info.blockchain.wallet.coin.GenericMetadataAccount;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;

public final class LabelUtil {

    private LabelUtil() {
        throw new AssertionError("This is a Util class and should not be instantiated");
    }

    /**
     * Returns false if the supplied label is not found to already belong to an {@link Account} or
     * imported {@link LegacyAddress}. Please note that this method ignores casing.
     *
     * @param payloadDataManager The current {@link PayloadDataManager}
     * @param bchDataManager     The current {@link BchDataManager}
     * @param newLabel           The label to be checked
     * @return A boolean, where true represents the label not being unique
     */
    public static boolean isExistingLabel(PayloadDataManager payloadDataManager,
                                          BchDataManager bchDataManager,
                                          String newLabel) {

        for (Account account : payloadDataManager.getAccounts()) {
            if (account.getLabel() != null
                    && account.getLabel().equalsIgnoreCase(newLabel)) {
                return true;
            }
        }

        for (GenericMetadataAccount account : bchDataManager.getAccountMetadataList()) {
            if (account.getLabel() != null
                    && account.getLabel().equalsIgnoreCase(newLabel)) {
                return true;
            }
        }

        for (LegacyAddress legacyAddress : payloadDataManager.getLegacyAddresses()) {
            if (legacyAddress.getLabel() != null
                    && legacyAddress.getLabel().equalsIgnoreCase(newLabel)) {
                return true;
            }
        }

        return false;
    }

}
