package piuk.blockchain.android.ui.receive;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;

import piuk.blockchain.android.util.MonetaryUtil;

class AddressBalanceHelper {

    private MonetaryUtil monetaryUtil;
    private MultiAddrFactory multiAddrFactory;

    AddressBalanceHelper(MonetaryUtil monetaryUtil, MultiAddrFactory multiAddrFactory) {
        this.monetaryUtil = monetaryUtil;
        this.multiAddrFactory = multiAddrFactory;
    }

    /**
     * Returns the balance of an {@link Account} in Satoshis
     */
    long getAccountAbsoluteBalance(Account account) {
        return multiAddrFactory.getXpubAmounts().get(account.getXpub());
    }

    /**
     * Returns the balance of an {@link Account}, formatted for display
     */
    String getAccountBalance(Account account, boolean isBTC, double btcExchange, String fiatUnit, String btcUnit) {

        long btcBalance = getAccountAbsoluteBalance(account);

        if (!isBTC) {
            double fiatBalance = btcExchange * (btcBalance / 1e8);
            return "(" + monetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance) + " " + fiatUnit + ")";
        } else {
            return "(" + monetaryUtil.getDisplayAmount(btcBalance) + " " + btcUnit + ")";
        }
    }

    /**
     * Returns the balance of a {@link LegacyAddress} in Satoshis
     */
    long getAddressAbsoluteBalance(LegacyAddress legacyAddress) {
        return multiAddrFactory.getLegacyBalance(legacyAddress.getAddress());
    }

    /**
     * Returns the balance of a {@link LegacyAddress}, formatted for display
     */
    String getAddressBalance(LegacyAddress legacyAddress, boolean isBTC, double btcExchange, String fiatUnit, String btcUnit) {

        long btcBalance = getAddressAbsoluteBalance(legacyAddress);

        if (!isBTC) {
            double fiatBalance = btcExchange * (btcBalance / 1e8);
            return "(" + monetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance) + " " + fiatUnit + ")";
        } else {
            return "(" + monetaryUtil.getDisplayAmount(btcBalance) + " " + btcUnit + ")";
        }
    }
}
