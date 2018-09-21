package info.blockchain.wallet.payload.data;

import com.blockchain.serialization.JsonSerializableAccount;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class Account implements JsonSerializableAccount {

    @JsonProperty("label")
    private String label = "";

    @JsonProperty("archived")
    private boolean archived;

    @JsonProperty("xpriv")
    private String xpriv;

    @JsonProperty("xpub")
    private String xpub;

    @JsonProperty("cache")
    private Cache cache;

    @JsonProperty("address_labels")
    private List<AddressLabel> addressLabels;

    public String getLabel() {
        return label;
    }

    public Account() {
        addressLabels = new ArrayList<>();
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getXpriv() {
        return xpriv;
    }

    public void setXpriv(String xpriv) {
        this.xpriv = xpriv;
    }

    public String getXpub() {
        return xpub;
    }

    public void setXpub(String xpub) {
        this.xpub = xpub;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public List<AddressLabel> getAddressLabels() {
        return addressLabels;
    }

    public void setAddressLabels(
        List<AddressLabel> addressLabels) {
        this.addressLabels = addressLabels;
    }

    public static Account fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, Account.class);
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public void addAddressLabel(int index, String reserveLabel) {
        AddressLabel addressLabel = new AddressLabel();
        addressLabel.setLabel(reserveLabel);
        addressLabel.setIndex(index);

        if(!addressLabels.contains(addressLabel)) {
            addressLabels.add(addressLabel);
        }
    }
}
