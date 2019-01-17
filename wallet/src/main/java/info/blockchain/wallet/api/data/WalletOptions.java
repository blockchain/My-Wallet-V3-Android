package info.blockchain.wallet.api.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletOptions {

    @JsonProperty("showBuySellTab")
    private List<String> buySellCountries = new ArrayList<>();

    @JsonProperty("partners")
    private Partners partners;

    @JsonProperty("androidBuyPercent")
    private double rolloutPercentage;

    @JsonProperty("android")
    private Map<String, Boolean> androidFlags = new HashMap<>();

    @JsonProperty("ethereum")
    private EthereumOptions ethereum;

    @JsonProperty("androidUpgrade")
    private Map<String, Integer> androidUpgrade = new HashMap<>();

    @JsonProperty("mobileInfo")
    private Map<String, String> mobileInfo = new HashMap<>();

    @JsonProperty("bcash")
    private Map<String, Integer> bitcoinCashFees = new HashMap<>();

    @JsonProperty("mobile")
    private Map<String, String> mobile = new HashMap<>();

    @JsonProperty("domains")
    private Map<String, String> domains = new HashMap<>();

    public List<String> getBuySellCountries() {
        return buySellCountries;
    }

    public Partners getPartners() {
        return partners;
    }

    public double getRolloutPercentage() {
        return rolloutPercentage;
    }

    public Map<String, Boolean> getAndroidFlags() {
        return androidFlags;
    }

    public EthereumOptions getEthereum() {
        return ethereum;
    }

    public Map<String, Integer> getAndroidUpgrade() {
        return androidUpgrade;
    }

    public Map<String, String> getMobileInfo() {
        return mobileInfo;
    }

    public int getBchFeePerByte() {
        return bitcoinCashFees.get("feePerByte");
    }

    @Nullable
    public String getBuyWebviewWalletLink() {
        return mobile.get("walletRoot");
    }

    public String getComRootLink() {
        return domains.get("comRoot");
    }

    public String getWalletLink() {
        return domains.get("comWalletApp");
    }
}
