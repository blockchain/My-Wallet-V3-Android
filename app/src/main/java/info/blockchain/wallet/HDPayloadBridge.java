package info.blockchain.wallet;

import android.content.Context;
import android.content.Intent;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.util.AddressFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;

//import android.util.Log;

public class HDPayloadBridge {

    private static Context context = null;
    private static HDPayloadBridge instance = null;

    private HDPayloadBridge() {
        ;
    }

    public static HDPayloadBridge getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new HDPayloadBridge();
        }

        return instance;
    }

    public static HDPayloadBridge getInstance() {

        if (instance == null) {
            instance = new HDPayloadBridge();
        }

        return instance;
    }

    public boolean init(CharSequenceX password) {

        PayloadFactory.getInstance().get(PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_GUID, ""),
                AppUtil.getInstance(context).getSharedKey(),
                password);

        if (PayloadFactory.getInstance().get() == null || PayloadFactory.getInstance().get().stepNumber != 9) {
            String error = context.getString(R.string.cannot_create_wallet);
            if (PayloadFactory.getInstance().get() != null) {
                error = error + " Failed at step: " + PayloadFactory.getInstance().get().stepNumber;
                if (PayloadFactory.getInstance().get().lastErrorMessage != null) {
                    error = error + " with message: " + PayloadFactory.getInstance().get().lastErrorMessage;
                }
            }
            ToastCustom.makeText(context, error, ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            return false;
        }

        if (PayloadFactory.getInstance().get().getJSON() == null) {
            ToastCustom.makeText(context, context.getString(R.string.please_repair),
                    ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            return false;
        }

        return true;
    }

    public boolean update(CharSequenceX password, CharSequenceX secondPassword) throws IOException, JSONException,
            DecoderException, AddressFormatException, MnemonicException.MnemonicLengthException,
            MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicWordException {

        if (PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_ASK_LATER, false)) {
            return true;
        }

        if (!PayloadFactory.getInstance().get().isUpgraded() &&
                PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L) == 0L) {
            return true;
        }

        if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            if (StringUtils.isEmpty(secondPassword) || !DoubleEncryptionFactory.getInstance().validateSecondPassword(
                    PayloadFactory.getInstance().get().getDoublePasswordHash(),
                    PayloadFactory.getInstance().get().getSharedKey(),
                    new CharSequenceX(secondPassword),
                    PayloadFactory.getInstance().get().getOptions().getIterations())) {
                ToastCustom.makeText(context, context.getString(R.string.double_encryption_password_error),
                        ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                return false;
            }
        }
        //
        // create HD wallet and sync w/ payload
        //
        if (PayloadFactory.getInstance().get().getHdWallets() == null ||
                PayloadFactory.getInstance().get().getHdWallets().size() == 0) {

            AppUtil.getInstance(context).applyPRNGFixes();

            String xpub = null;
            int attempts = 0;
            boolean no_tx = false;

            do {

                attempts++;

                WalletFactory.getInstance().newWallet(12, "", 1);
                HDWallet hdw = new HDWallet();
                String seedHex = WalletFactory.getInstance().get().getSeedHex();
                if (!StringUtils.isEmpty(secondPassword)) {
                    seedHex = DoubleEncryptionFactory.getInstance().encrypt(
                            seedHex,
                            PayloadFactory.getInstance().get().getSharedKey(),
                            secondPassword.toString(),
                            PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations());
                }

                hdw.setSeedHex(seedHex);
                List<Account> accounts = new ArrayList<Account>();
                xpub = WalletFactory.getInstance().get().getAccount(0).xpubstr();
                if (AppUtil.getInstance(context).isNewlyCreated()) {
                    accounts.add(new Account());
                    accounts.get(0).setXpub(xpub);
                    String xpriv = WalletFactory.getInstance().get().getAccount(0).xprvstr();
                    if (!StringUtils.isEmpty(secondPassword)) {
                        xpriv = DoubleEncryptionFactory.getInstance().encrypt(
                                xpriv,
                                PayloadFactory.getInstance().get().getSharedKey(),
                                secondPassword.toString(),
                                PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations());
                    }
                    accounts.get(0).setXpriv(xpriv);
                }
                hdw.setAccounts(accounts);
                PayloadFactory.getInstance().get().setHdWallets(hdw);
                PayloadFactory.getInstance().get().setUpgraded(true);

                PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(
                        context.getResources().getString(R.string.default_wallet_name));

                no_tx = (MultiAddrFactory.getInstance().nbTxXPUB(xpub) == 0L);

            } while (!no_tx && attempts < 3);

            if (!no_tx && AppUtil.getInstance(context).isNewlyCreated()) {
                return false;
            } else {
                if(!PayloadBridge.getInstance(context).remoteSaveThreadLocked())
                    return false;
            }
        }

        updateBalancesAndTransactions();
        List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
        PayloadFactory.getInstance().get().getHdWallet().setAccounts(accounts);
        PayloadFactory.getInstance().cache();

        if (OSUtil.getInstance(context.getApplicationContext()).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
            context.getApplicationContext().stopService(new Intent(context.getApplicationContext(),
                    info.blockchain.wallet.service.WebSocketService.class));
        }
        context.getApplicationContext().startService(new Intent(context.getApplicationContext(),
                info.blockchain.wallet.service.WebSocketService.class));

        return true;
    }

    public void updateBalancesAndTransactions() throws JSONException, IOException, DecoderException, AddressFormatException,
            MnemonicException.MnemonicLengthException, MnemonicException.MnemonicChecksumException,
            MnemonicException.MnemonicWordException {
        // TODO unify legacy and HD call to one API call

        // Balance for legacy addresses
        if (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
            List<String> legacyAddresses = PayloadFactory.getInstance().get().getLegacyAddressStrings();
            String[] addresses = legacyAddresses.toArray(new String[legacyAddresses.size()]);
            MultiAddrFactory.getInstance().getLegacy(addresses, false);
        }

        // xPub balance
        if (!AppUtil.getInstance(context).isNotUpgraded()) {
            String[] xpubs = getXPUBs(false);
            if (xpubs.length > 0) {
                MultiAddrFactory.getInstance().getXPUB(xpubs);
            }
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
            for (Account a : accounts) {
                a.setIdxReceiveAddresses(MultiAddrFactory.getInstance().getHighestTxReceiveIdx(a.getXpub()) > a.getIdxReceiveAddresses() ?
                        MultiAddrFactory.getInstance().getHighestTxReceiveIdx(a.getXpub()) : a.getIdxReceiveAddresses());
                a.setIdxChangeAddresses(MultiAddrFactory.getInstance().getHighestTxChangeIdx(a.getXpub()) > a.getIdxChangeAddresses() ?
                        MultiAddrFactory.getInstance().getHighestTxChangeIdx(a.getXpub()) : a.getIdxChangeAddresses());
            }
        }
    }

    public String getHDSeed() throws IOException, MnemonicException.MnemonicLengthException {
        return WalletFactory.getInstance().get().getSeedHex();
    }

    public String getHDMnemonic() throws IOException, MnemonicException.MnemonicLengthException {
        return WalletFactory.getInstance().get().getMnemonic();
    }

    public String getHDPassphrase() throws IOException, MnemonicException.MnemonicLengthException {
        return WalletFactory.getInstance().get().getPassphrase();
    }

    public ReceiveAddress getReceiveAddress(int accountIdx) throws DecoderException, IOException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, AddressFormatException {

        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            return AddressFactory.getInstance(context, null).getReceiveAddress(accountIdx);
        } else {
            return AddressFactory.getInstance(context, getXPUBs(true)).getReceiveAddress(accountIdx);
        }

    }

    public String account2Xpub(int accountIdx) {

        return PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getXpub();

    }

    public void createHDWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException {
        WalletFactory.getInstance().newWallet(12, passphrase, 1);
        PayloadBridge.getInstance(context).createBlockchainWallet(WalletFactory.getInstance().get());
    }

    public void restoreHDWallet(String seed, String passphrase, int nbAccounts) throws IOException, AddressFormatException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException {
        WalletFactory.getInstance().restoreWallet(seed, passphrase, 1);
        PayloadBridge.getInstance(context).createBlockchainWallet(WalletFactory.getInstance().get());
    }

    //
    //
    //
    private String[] getXPUBs(boolean includeArchives) throws IOException, DecoderException, AddressFormatException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicWordException {

        ArrayList<String> xpubs = new ArrayList<String>();

        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {

            org.bitcoinj.core.bip44.Wallet hd_wallet = null;

            if (PayloadFactory.getInstance().get().getHdWallet() != null) {
                hd_wallet = WalletFactory.getInstance().restoreWallet(PayloadFactory.getInstance().get().getHdWallet().getSeedHex(), PayloadFactory.getInstance().get().getHdWallet().getPassphrase(), PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
            }

        }

        //
        // null test added for 'lame' mode
        //
        if (PayloadFactory.getInstance().get().getHdWallet() != null) {
            int nb_accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts().size();
            for (int i = 0; i < nb_accounts; i++) {
                boolean isArchived = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(i).isArchived();
                if (isArchived && !includeArchives) {
                    ;
                } else {
                    String s = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(i).getXpub();
                    if (s != null && s.length() > 0) {
                        xpubs.add(s);
                    }
                }
            }
        }

        return xpubs.toArray(new String[xpubs.size()]);
    }
}
