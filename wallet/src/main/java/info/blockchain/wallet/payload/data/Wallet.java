package info.blockchain.wallet.payload.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.NoSuchAddressException;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FormatsUtil;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class Wallet {

    @JsonProperty("guid")
    private String guid;

    @JsonProperty("sharedKey")
    private String sharedKey;

    @JsonProperty("double_encryption")
    private boolean doubleEncryption;

    @JsonProperty("dpasswordhash")
    private String dpasswordhash;

    @JsonProperty("metadataHDNode")
    private String metadataHDNode;

    @JsonProperty("tx_notes")
    private Map<String, String> txNotes;

    @JsonProperty("tx_tags")
    private Map<String, List<Integer>> txTags;

    @JsonProperty("tag_names")
    private List<Map<Integer, String>> tagNames;

    @JsonProperty("options")
    private Options options;

    @JsonProperty("wallet_options")
    private Options walletOptions;

    @JsonProperty("hd_wallets")
    private List<HDWallet> hdWallets;

    @JsonProperty("keys")
    private List<LegacyAddress> keys;

    @JsonProperty("address_book")
    private List<AddressBook> addressBook;

    public Wallet() {
        guid = UUID.randomUUID().toString();
        sharedKey = UUID.randomUUID().toString();
        txNotes = new HashMap<>();
        keys = new ArrayList<>();
        options = Options.getDefaultOptions();
    }

    public Wallet(String defaultAccountName) throws Exception {

        guid = UUID.randomUUID().toString();
        sharedKey = UUID.randomUUID().toString();
        txNotes = new HashMap<>();
        keys = new ArrayList<>();
        options = Options.getDefaultOptions();

        HDWallet hdWalletBody = new HDWallet(defaultAccountName);

        hdWallets = new ArrayList<>();
        hdWallets.add(hdWalletBody);
    }

    public String getGuid() {
        return guid;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public boolean isDoubleEncryption() {
        return doubleEncryption;
    }

    public String getDpasswordhash() {
        return dpasswordhash;
    }

    public String getMetadataHDNode() {
        return metadataHDNode;
    }

    public Map<String, String> getTxNotes() {
        return txNotes;
    }

    public Map<String, List<Integer>> getTxTags() {
        return txTags;
    }

    public List<Map<Integer, String>> getTagNames() {
        return tagNames;
    }

    public Options getOptions() {
        fixPbkdf2Iterations();
        return options;
    }

    public Options getWalletOptions() {
        return walletOptions;
    }

    public List<HDWallet> getHdWallets() {
        return hdWallets;
    }

    public List<LegacyAddress> getLegacyAddressList() {
        return keys;
    }

    public List<AddressBook> getAddressBook() {
        return addressBook;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public void setDoubleEncryption(boolean doubleEncryption) {
        this.doubleEncryption = doubleEncryption;
    }

    public void setDpasswordhash(String dpasswordhash) {
        this.dpasswordhash = dpasswordhash;
    }

    public void setMetadataHDNode(String metadataHDNode) {
        this.metadataHDNode = metadataHDNode;
    }

    public void setTxNotes(Map<String, String> txNotes) {
        this.txNotes = txNotes;
    }

    public void setTxTags(Map<String, List<Integer>> txTags) {
        this.txTags = txTags;
    }

    public void setTagNames(List<Map<Integer, String>> tagNames) {
        this.tagNames = tagNames;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public void setWalletOptions(Options walletOptions) {
        this.walletOptions = walletOptions;
    }

    public void setHdWallets(List<HDWallet> hdWallets) {
        this.hdWallets = hdWallets;
    }

    public void setLegacyAddressList(List<LegacyAddress> keys) {
        this.keys = keys;
    }

    public void setAddressBook(List<AddressBook> addressBook) {
        this.addressBook = addressBook;
    }

    public boolean isUpgraded() {
        return (hdWallets != null && hdWallets.size() > 0);
    }

    public static Wallet fromJson(NetworkParameters networkParameters, String json)
        throws IOException, HDWalletException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        Wallet wallet = mapper.readValue(json, Wallet.class);

        if(wallet.getHdWallets() != null) {
            //V3 Wallets only
            Iterator<HDWallet> iterator = wallet.getHdWallets().iterator();

            ArrayList<HDWallet> hdWalletList = new ArrayList<>();
            while (iterator.hasNext()) {
                HDWallet nextHd = iterator.next();
                hdWalletList.add(HDWallet.fromJson(networkParameters, nextHd.toJson()));

            }
            wallet.setHdWallets(hdWalletList);
        }

        return wallet;
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public void addHDWallet(HDWallet hdWallet) {

        if (hdWallets == null) {
            hdWallets = new ArrayList<>();
        }

        hdWallets.add(hdWallet);
    }

    /**
     * Checks imported address and hd keys for possible double encryption corruption
     */
    public boolean isEncryptionConsistent() {
        ArrayList<String> keyList = new ArrayList<>();

        if (getLegacyAddressList() != null) {
            List<LegacyAddress> legacyAddresses = getLegacyAddressList();
            for (LegacyAddress legacyAddress : legacyAddresses) {
                if (!legacyAddress.isWatchOnly()) {
                    keyList.add(legacyAddress.getPrivateKey());
                }
            }
        }

        if (getHdWallets() != null && getHdWallets().size() > 0) {

            for (HDWallet hdWallet : getHdWallets()) {
                List<Account> accounts = hdWallet.getAccounts();
                for (Account account : accounts) {
                    keyList.add(account.getXpriv());
                }
            }
        }

        return isEncryptionConsistent(isDoubleEncryption(), keyList);
    }

    public boolean isEncryptionConsistent(boolean isDoubleEncrypted, List<String> keyList) {

        boolean consistent = true;

        for (String key : keyList) {

            if (isDoubleEncrypted) {
                consistent = FormatsUtil.isKeyEncrypted(key);
            } else {
                consistent = FormatsUtil.isKeyUnencrypted(key);
            }

            if (!consistent) {
                break;
            }
        }

        return consistent;
    }

    public void validateSecondPassword(@Nullable String secondPassword) throws DecryptionException {

        if(isDoubleEncryption()) {
            DoubleEncryptionFactory.validateSecondPassword(
                getDpasswordhash(),
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations());
        } else if(!isDoubleEncryption() && secondPassword != null) {
            throw new DecryptionException("Double encryption password specified on non double encrypted wallet.");
        }
    }

    public void upgradeV2PayloadToV3(@Nullable String secondPassword, String defaultAccountName) throws Exception {

        //Check if payload has 2nd password
        validateSecondPassword(secondPassword);

        if (!isUpgraded()) {

            //Create new hd wallet
            HDWallet hdWalletBody = new HDWallet(defaultAccountName);
            addHDWallet(hdWalletBody);

            //Double encrypt if need
            if (!StringUtils.isEmpty(secondPassword)) {

                //Double encrypt seedHex
                String doubleEncryptedSeedHex = DoubleEncryptionFactory.encrypt(
                    hdWalletBody.getSeedHex(),
                    getSharedKey(),
                    secondPassword,
                    getOptions().getPbkdf2Iterations());
                hdWalletBody.setSeedHex(doubleEncryptedSeedHex);

                //Double encrypt private keys
                for(Account account : hdWalletBody.getAccounts()) {

                    String encryptedXPriv = DoubleEncryptionFactory.encrypt(
                        account.getXpriv(),
                        getSharedKey(),
                        secondPassword,
                        getOptions().getPbkdf2Iterations());

                    account.setXpriv(encryptedXPriv);

                }
            }
        }
    }

    public LegacyAddress addLegacyAddress(String label, @Nullable String secondPassword)
        throws Exception {
        validateSecondPassword(secondPassword);
        LegacyAddress addressBody = LegacyAddress.generateNewLegacy();
        addressBody.setLabel(label);

        if(secondPassword != null) {
            //Double encryption
            String unencryptedKey = addressBody.getPrivateKey();

            String encryptedKey = DoubleEncryptionFactory.encrypt(unencryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations());

            addressBody.setPrivateKey(encryptedKey);

        }

        keys.add(addressBody);

        return addressBody;
    }

    public LegacyAddress addLegacyAddressFromKey(ECKey key, @Nullable String secondPassword)
        throws Exception {
        validateSecondPassword(secondPassword);
        LegacyAddress addressBody = LegacyAddress.fromECKey(key);

        if(secondPassword != null) {
            //Double encryption
            String unencryptedKey = addressBody.getPrivateKey();

            String encryptedKey = DoubleEncryptionFactory.encrypt(unencryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations());

            addressBody.setPrivateKey(encryptedKey);

        }

        keys.add(addressBody);

        return addressBody;
    }

    public void decryptHDWallet(NetworkParameters networkParameters, int hdWalletIndex, String secondPassword)
        throws MnemonicWordException, DecryptionException, IOException, DecoderException,
        MnemonicChecksumException, MnemonicLengthException, InvalidCipherTextException, HDWalletException {

        validateSecondPassword(secondPassword);

        HDWallet hdWallet = hdWallets.get(hdWalletIndex);
        hdWallet.decryptHDWallet(networkParameters, secondPassword, sharedKey, getOptions().getPbkdf2Iterations());
    }

    private void encryptAccount(Account account, String secondPassword)
        throws UnsupportedEncodingException, EncryptionException {
        //Double encryption
        if(secondPassword != null) {
            String encryptedPrivateKey = DoubleEncryptionFactory.encrypt(
                account.getXpriv(),
                sharedKey,
                secondPassword,
                getOptions().getPbkdf2Iterations());
            account.setXpriv(encryptedPrivateKey);
        }
    }

    public Account addAccount(NetworkParameters networkParameters, int hdWalletIndex, String label, @Nullable String secondPassword)
        throws Exception {

        validateSecondPassword(secondPassword);

        //Double decryption if need
        decryptHDWallet(networkParameters, hdWalletIndex, secondPassword);

        HDWallet hdWallet = hdWallets.get(hdWalletIndex);

        Account account = hdWallet.addAccount(label);

        //Double encryption if need
        encryptAccount(account, secondPassword);

        return account;
    }

    public LegacyAddress setKeyForLegacyAddress(ECKey key, @Nullable String secondPassword)
        throws DecryptionException, UnsupportedEncodingException, EncryptionException,
        NoSuchAddressException {

        validateSecondPassword(secondPassword);

        List<LegacyAddress> addressList = getLegacyAddressList();

        String address = key.toAddress(PersistentUrls.getInstance().getBitcoinParams()).toString();

        LegacyAddress matchingAddressBody = null;

        for(LegacyAddress addressBody : addressList) {
            if(addressBody.getAddress().equals(address)) {
                matchingAddressBody = addressBody;
            }
        }

        if(matchingAddressBody == null) {
            throw new NoSuchAddressException("No matching address found for key");
        }

        if(secondPassword != null) {
            //Double encryption
            String encryptedKey = Base58.encode(key.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.encrypt(encryptedKey,
                    getSharedKey(),
                    secondPassword,
                    getOptions().getPbkdf2Iterations());

            matchingAddressBody.setPrivateKey(encrypted2);

        } else {
            matchingAddressBody.setPrivateKeyFromBytes(key.getPrivKeyBytes());
        }

        return matchingAddressBody;
    }

    public List<String> getLegacyAddressStringList() {

        List<String> addrs = new ArrayList<>(keys.size());
        for (LegacyAddress legacyAddress : keys) {
            if (!LegacyAddressExtensionsKt.isArchived(legacyAddress)) {
                addrs.add(legacyAddress.getAddress());
            }
        }

        return addrs;
    }

    public List<String> getWatchOnlyAddressStringList() {

        List<String> addrs = new ArrayList<>(keys.size());
        for (LegacyAddress legacyAddress : keys) {
            if (legacyAddress.isWatchOnly()) {
                addrs.add(legacyAddress.getAddress());
            }
        }

        return addrs;
    }

    public List<String> getLegacyAddressStringList(long tag) {

        List<String> addrs = new ArrayList<>(keys.size());
        for (LegacyAddress legacyAddress : keys) {
            if (legacyAddress.getTag() == tag) {
                addrs.add(legacyAddress.getAddress());
            }
        }

        return addrs;
    }

    public boolean containsLegacyAddress(String addr) {

        for (LegacyAddress legacyAddress : keys) {
            if (legacyAddress.getAddress().equals(addr)) {
                return true;
            }
        }

        return false;
    }

    /**
     * In case wallet was encrypted with iterations other than what is specified in options, we
     * will ensure next encryption and options get updated accordingly.
     * @return
     */
    private int fixPbkdf2Iterations() {

        //Use default initially
        int iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2;

        //Old wallets may contain 'wallet_options' key - we'll use this now
        if (walletOptions != null && walletOptions.getPbkdf2Iterations() > 0) {
            iterations = walletOptions.getPbkdf2Iterations();
            options.setPbkdf2Iterations(iterations);
        }

        //'options' key override wallet_options key - we'll use this now
        if (options != null && options.getPbkdf2Iterations() > 0) {
            iterations = options.getPbkdf2Iterations();
        }

        //If wallet doesn't contain 'option' - use default
        if(options == null) {
            options = Options.getDefaultOptions();
        }

        //Set iterations
        options.setPbkdf2Iterations(iterations);

        return iterations;
    }

    /**
     * Returns label if match found, otherwise just returns address.
     * @param address
     * @return
     */
    public String getLabelFromLegacyAddress(String address) {

        List<LegacyAddress> addresses = getLegacyAddressList();

        for(LegacyAddress legacyAddress : addresses) {
            if(legacyAddress.getAddress().equals(address)) {
                String label = legacyAddress.getLabel();
                if(label == null || label.isEmpty()){
                    return address;
                } else {
                    return label;
                }
            }
        }

        return address;
    }
}