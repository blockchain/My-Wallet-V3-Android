package info.blockchain.wallet.payload.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.WalletApiAccess;
import info.blockchain.wallet.util.Util;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import okhttp3.ResponseBody;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.spongycastle.util.encoders.Hex;
import retrofit2.Call;
import retrofit2.Response;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class LegacyAddress {

    public static final int NORMAL_ADDRESS = 0;
    public static final int ARCHIVED_ADDRESS = 2;

    @JsonProperty("addr")
    private String address;

    @JsonProperty("priv")
    private String privateKey;

    @JsonProperty("label")
    private String label;

    @JsonProperty("created_time")
    private long createdTime;

    @JsonProperty("tag")
    private int tag;

    @JsonProperty("created_device_name")
    private String createdDeviceName;

    @JsonProperty("created_device_version")
    private String createdDeviceVersion;

    public String getAddress() {
        return address;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public boolean isPrivateKeyEncrypted() {
        try {
            Base58.decode(privateKey);
            return false;
        }catch (Exception e){
            return true;
        }
    }

    public String getLabel() {
        return label;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public int getTag() {
        return tag;
    }

    public String getCreatedDeviceName() {
        return createdDeviceName;
    }

    public String getCreatedDeviceVersion() {
        return createdDeviceVersion;
    }

    public boolean isWatchOnly() {
        return (privateKey == null);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAddressFromPublicKeyBytes(byte[] pubKeyBytes) {
        this.address = Base58.encode(pubKeyBytes);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPrivateKeyFromBytes(byte[] privKeyBytes) {
        this.privateKey = Base58.encode(privKeyBytes);
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public void setCreatedDeviceName(String createdDeviceName) {
        this.createdDeviceName = createdDeviceName;
    }

    public void setCreatedDeviceVersion(String createdDeviceVersion) {
        this.createdDeviceVersion = createdDeviceVersion;
    }

    public static LegacyAddress fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, LegacyAddress.class);
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public static List<LegacyAddress> filterAddress(int filter, @Nonnull List<LegacyAddress> keys) {

        List<LegacyAddress> addressList = new ArrayList<>();

        for(LegacyAddress key : keys) {
            if(key.getTag() == filter) {
                addressList.add(key);
            }
        }

        return addressList;
    }

    public static LegacyAddress generateNewLegacy() throws Exception {

        ECKey ecKey = getRandomECKey();

        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setPrivateKeyFromBytes(ecKey.getPrivKeyBytes());
        legacyAddress.setAddress(ecKey.toAddress(PersistentUrls.getInstance().getBitcoinParams()).toString());
        legacyAddress.setCreatedDeviceName(BlockchainFramework.getDevice());
        legacyAddress.setCreatedTime(System.currentTimeMillis());
        legacyAddress.setCreatedDeviceVersion(BlockchainFramework.getAppVersion());

        return legacyAddress;
    }

    public static LegacyAddress fromECKey(ECKey ecKey) throws Exception {

        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setPrivateKeyFromBytes(ecKey.getPrivKeyBytes());

        legacyAddress.setAddress(ecKey.toAddress(PersistentUrls.getInstance().getBitcoinParams()).toBase58());
        legacyAddress.setCreatedDeviceName(BlockchainFramework.getDevice());
        legacyAddress.setCreatedTime(System.currentTimeMillis());
        legacyAddress.setCreatedDeviceVersion(BlockchainFramework.getAppVersion());

        return legacyAddress;
    }

    private static ECKey getRandomECKey() throws Exception {
        // TODO: Unused apart from in tests, remove as part of AND-1194
        Call<ResponseBody> call = WalletApiAccess.INSTANCE.getWalletApi().getRandomBytesCall();
        Response<ResponseBody> exe = call.execute();

        if(!exe.isSuccessful()){
            throw new Exception("ExternalEntropy.getRandomBytesCall failed.");
        }

        byte[] data = Hex.decode(exe.body().string());

        if (data == null) throw new Exception("ExternalEntropy.getRandomBytesCall failed.");

        byte[] rdata = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(rdata);
        byte[] privbytes = Util.xor(data, rdata);
        if (privbytes == null) {
            return null;
        }
        ECKey ecKey = ECKey.fromPrivate(privbytes, true);
        // erase all byte arrays:
        random.nextBytes(privbytes);
        random.nextBytes(rdata);
        random.nextBytes(data);

        return ecKey;
    }
}
