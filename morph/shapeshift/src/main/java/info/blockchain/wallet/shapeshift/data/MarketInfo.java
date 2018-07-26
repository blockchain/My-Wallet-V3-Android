package info.blockchain.wallet.shapeshift.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class MarketInfo {

    @JsonProperty("pair")
    private String pair;

    @JsonProperty("rate")
    private BigDecimal rate;

    @JsonProperty("limit")
    private BigDecimal limit;

    @JsonProperty("minimum")
    private BigDecimal minimum;

    @JsonProperty("maxLimit")
    private BigDecimal maxLimit;

    @JsonProperty("minerFee")
    private BigDecimal minerFee;

    public String getPair() {
        return pair;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public BigDecimal getMaxLimit() {
        return maxLimit;
    }

    public BigDecimal getMinerFee() {
        return minerFee;
    }

    public static MarketInfo fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, MarketInfo.class);
    }

    @JsonIgnore
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}
