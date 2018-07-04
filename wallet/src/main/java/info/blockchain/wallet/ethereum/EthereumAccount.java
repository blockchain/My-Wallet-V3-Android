package info.blockchain.wallet.ethereum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import info.blockchain.wallet.ethereum.util.HashUtil;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;

import java.util.Arrays;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class EthereumAccount {

    private static final String DERIVATION_PATH = "m/44'/60'/0'/0";
    private static final int DERIVATION_PATH_PURPOSE = 44;
    private static final int DERIVATION_PATH_COIN = 60;
    private static final int CHANGE_INDEX = 0;
    private static final int ADDRESS_INDEX = 0;

    @JsonProperty("archived")
    private boolean archived;

    @JsonProperty("label")
    private String label;

    @JsonProperty("addr")
    private String address;

    @JsonProperty("correct")
    private boolean correct;

    public EthereumAccount() {
        //default constructor for Jackson
    }

    public EthereumAccount(ECKey addressKey) {
        this.address = HashUtil.toHexString(computeAddress(addressKey.getPubKeyPoint().getEncoded(false)));
    }

    public static EthereumAccount deriveAccount(DeterministicKey masterKey, int accountIndex, String label) {
        EthereumAccount ethereumAccount = new EthereumAccount(deriveECKey(masterKey, accountIndex));
        ethereumAccount.setLabel(label);
        ethereumAccount.setCorrect(true);
        return ethereumAccount;
    }

    public static ECKey deriveECKey(DeterministicKey masterKey, int accountIndex) {

        DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(masterKey, DERIVATION_PATH_PURPOSE | ChildNumber.HARDENED_BIT);
        DeterministicKey rootKey = HDKeyDerivation.deriveChildKey(purposeKey, DERIVATION_PATH_COIN | ChildNumber.HARDENED_BIT);
        DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(rootKey, accountIndex | ChildNumber.HARDENED_BIT);
        DeterministicKey changeKey = HDKeyDerivation.deriveChildKey(accountKey, CHANGE_INDEX);
        DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(changeKey, ADDRESS_INDEX);

        return ECKey.fromPrivate(addressKey.getPrivKeyBytes());
    }

    //TODO: 24/08/2017 Case sensitivity is used for optional checksum
    //TODO: 24/08/2017 https://forum.ethereum.org/discussion/9220/eth-address-upper-and-lower-characters-does-not-matter
    public String getAddress() {
        return address;
    }

    /**
     * Compute an address from an encoded public key.
     *
     * @param pubBytes an encoded (uncompressed) public key
     * @return 20-byte address
     */
    private byte[] computeAddress(byte[] pubBytes) {
        return HashUtil.sha3omit12(Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    /**
     * @param transaction
     * @return Signed transaction bytes
     */
    public byte[] signTransaction(RawTransaction transaction, ECKey accountKey) {
        Credentials credentials = Credentials.create(accountKey.getPrivateKeyAsHex());
        return TransactionEncoder.signMessage(transaction, credentials);
    }
}
