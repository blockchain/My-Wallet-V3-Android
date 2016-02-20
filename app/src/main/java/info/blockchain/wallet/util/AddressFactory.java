package info.blockchain.wallet.util;

import android.content.Context;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.Address;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;

import piuk.blockchain.android.R;

public class AddressFactory {

    public static final int RECEIVE_CHAIN = 0;

    private static Wallet double_encryption_wallet = null;

    private static Context context = null;
    private static AddressFactory instance = null;

    private AddressFactory() {
        ;
    }

    public static AddressFactory getInstance(Context ctx, String[] xpub) throws AddressFormatException {
        context = ctx;

        if (instance == null) {

            if (xpub != null) {
                double_encryption_wallet = WalletFactory.getInstance(xpub).getWatchOnlyWallet();
            }

            instance = new AddressFactory();
        }

        return instance;
    }

    public void updateDoubleEncryptionWallet(){
        double_encryption_wallet = WalletFactory.getInstance().getWatchOnlyWallet();
    }

    public ReceiveAddress getReceiveAddress(int accountIdx) {
        int idx = 0;
        Address addr = null;

        try {
            idx = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getIdxReceiveAddresses();
            if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                addr = WalletFactory.getInstance().get().getAccount(accountIdx).getChain(AddressFactory.RECEIVE_CHAIN).getAddressAt(idx);
            } else {
                addr = double_encryption_wallet.getAccount(accountIdx).getChain(AddressFactory.RECEIVE_CHAIN).getAddressAt(idx);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

        ReceiveAddress ret = new ReceiveAddress(addr.getAddressString(), idx);

        return ret;
    }
}
