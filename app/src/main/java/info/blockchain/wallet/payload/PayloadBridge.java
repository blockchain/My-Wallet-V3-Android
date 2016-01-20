package info.blockchain.wallet.payload;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.Util;
import info.blockchain.wallet.util.WebUtil;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import piuk.blockchain.android.R;

//import android.util.Log;

/**
 * PayloadBridge.java : singleton class for remote save of payload and wallet creation
 */
public class PayloadBridge {


    private static Context context = null;

    private static PayloadBridge instance = null;

    private PayloadBridge() {
        ;
    }

    /**
     * Return instance for a PayloadBridge
     *
     * @param Context ctx app context
     * @return PayloadBridge
     */
    public static PayloadBridge getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new PayloadBridge();
        }

        return instance;
    }

    /**
     * Create a Blockchain wallet and include the HD_Wallet passed as an argument and write it to this instance's
     * payload.
     *
     * @param HD_Wallet hdw HD wallet to include in the payload
     * @return boolean
     */
    public boolean createBlockchainWallet(org.bitcoinj.core.bip44.Wallet hdw) {

        String guid = UUID.randomUUID().toString();
        String sharedKey = UUID.randomUUID().toString();

        Payload payload = new Payload();
        payload.setGuid(guid);
        payload.setSharedKey(sharedKey);

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, guid);
        AppUtil.getInstance(context).setSharedKey(sharedKey);

        HDWallet payloadHDWallet = new HDWallet();
        payloadHDWallet.setSeedHex(hdw.getSeedHex());

        List<org.bitcoinj.core.bip44.Account> hdAccounts = hdw.getAccounts();
        List<info.blockchain.wallet.payload.Account> payloadAccounts = new ArrayList<info.blockchain.wallet.payload.Account>();
        for (int i = 0; i < hdAccounts.size(); i++) {
            info.blockchain.wallet.payload.Account account = new info.blockchain.wallet.payload.Account(context.getString(R.string.default_wallet_name));
            try {
                String xpub = WalletFactory.getInstance().get().getAccounts().get(i).xpubstr();
                account.setXpub(xpub);
                String xpriv = WalletFactory.getInstance().get().getAccounts().get(i).xprvstr();
                account.setXpriv(xpriv);
            } catch (IOException | MnemonicException.MnemonicLengthException e) {
                e.printStackTrace();
            }

            payloadAccounts.add(account);
        }
        payloadHDWallet.setAccounts(payloadAccounts);

        payload.setHdWallets(payloadHDWallet);

        PayloadFactory.getInstance().set(payload);
        PayloadFactory.getInstance().setNew(true);

        return true;
    }

    /**
     * Thread for remote save of payload to server.
     */
    public boolean remoteSaveThreadLocked()  {

        if (PayloadFactory.getInstance().get() != null) {
            return PayloadFactory.getInstance().put();
        }else{
            return false;
        }
    }

    public void remoteSaveThread() {

        getSaveThread().execute();
    }

    private AsyncTask getSaveThread(){

        return new AsyncTask<Void, Void, Boolean>(){

            @Override
            protected Boolean doInBackground(Void... params) {
                if (PayloadFactory.getInstance().get() != null) {
                    return PayloadFactory.getInstance().put();
                }else{
                    ToastCustom.makeText(context, context.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    return false;
                }
            }
        };
    }

    /*
     *
     *
        For 'lame' mode only
     *
     *
     */
    public ECKey newLegacyAddress() {

        AppUtil.getInstance(context).applyPRNGFixes();

        String result = null;
        byte[] data = null;
        try {
            result = WebUtil.getInstance().getURL(WebUtil.EXTERNAL_ENTROPY_URL);
            if (!result.matches("^[A-Fa-f0-9]{64}$")) {
                return null;
            }
            data = Hex.decode(result);
        } catch (Exception e) {
            return null;
        }

        ECKey ecKey = null;
        if (data != null) {
            byte[] rdata = new byte[32];
            SecureRandom random = new SecureRandom();
            random.nextBytes(rdata);
            byte[] privbytes = Util.getInstance().xor(data, rdata);
            if (privbytes == null) {
                return null;
            }
            ecKey = ECKey.fromPrivate(privbytes, true);
            // erase all byte arrays:
            random.nextBytes(privbytes);
            random.nextBytes(rdata);
            random.nextBytes(data);
        } else {
            return null;
        }

        return ecKey;
    }

}