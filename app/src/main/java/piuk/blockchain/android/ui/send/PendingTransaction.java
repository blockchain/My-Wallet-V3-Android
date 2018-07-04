package piuk.blockchain.android.ui.send;

import com.fasterxml.jackson.annotation.JsonIgnore;

import info.blockchain.wallet.coin.GenericMetadataAccount;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import java.math.BigInteger;

import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies;

public class PendingTransaction {

    public static final int WATCH_ONLY_SPEND_TAG = -5;

    public SpendableUnspentOutputs unspentOutputBundle;
    public ItemAccount sendingObject;
    public ItemAccount receivingObject;
    public String note;
    public String receivingAddress;
    public String changeAddress;
    public BigInteger bigIntFee;
    public BigInteger bigIntAmount;
    public int addressToReceiveIndex;
    public String warningText;
    public String warningSubText;

    @JsonIgnore
    public BigInteger getTotal() {
        return bigIntAmount.add(bigIntFee);
    }

    @JsonIgnore
    public boolean isHD(CryptoCurrencies currency) {
        if (currency == CryptoCurrencies.BTC) {
            return (sendingObject.getAccountObject() instanceof Account);
        } else {
            return (sendingObject.getAccountObject() instanceof GenericMetadataAccount);
        }
    }

    @JsonIgnore
    public boolean isWatchOnly() {
        boolean watchOnly = false;

        if (sendingObject.getAccountObject() instanceof LegacyAddress) {
            LegacyAddress legacyAddress = (LegacyAddress) sendingObject.getAccountObject();
            watchOnly = legacyAddress.isWatchOnly() && (legacyAddress.getPrivateKey() == null || legacyAddress.getPrivateKey().isEmpty());
        }

        return watchOnly;
    }

    @JsonIgnore
    public String getDisplayableReceivingLabel() {
        if (receivingObject != null && receivingObject.getLabel() != null && !receivingObject.getLabel().isEmpty()) {
            return receivingObject.getLabel();
        } else {
            return receivingAddress;
        }
    }

    @JsonIgnore
    public void clear() {
        unspentOutputBundle = null;
        sendingObject = null;
        receivingAddress = null;
        note = null;
        receivingAddress = null;
        bigIntFee = null;
        bigIntAmount = null;
        warningText = null;
        warningSubText = null;
    }

    @Override
    public String toString() {
        return "PendingTransaction{" +
                "unspentOutputBundle=" + unspentOutputBundle +
                ", sendingObject=" + sendingObject +
                ", receivingObject=" + receivingObject +
                ", note='" + note + '\'' +
                ", receivingAddress='" + receivingAddress + '\'' +
                ", changeAddress='" + changeAddress + '\'' +
                ", bigIntFee=" + bigIntFee +
                ", bigIntAmount=" + bigIntAmount +
                ", addressToReceiveIndex=" + addressToReceiveIndex +
                ", warningText=" + warningText +
                ", warningSubText=" + warningSubText +
                '}';
    }
}
