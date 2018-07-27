package info.blockchain.wallet.payload.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.PersistentUrls;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;

import java.io.IOException;

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
        } catch (Exception e) {
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

    public static LegacyAddress fromECKey(ECKey ecKey) {

        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setPrivateKeyFromBytes(ecKey.getPrivKeyBytes());

        legacyAddress.setAddress(ecKey.toAddress(PersistentUrls.getInstance().getBitcoinParams()).toBase58());
        legacyAddress.setCreatedDeviceName(BlockchainFramework.getDevice());
        legacyAddress.setCreatedTime(System.currentTimeMillis());
        legacyAddress.setCreatedDeviceVersion(BlockchainFramework.getAppVersion());

        return legacyAddress;
    }
}
