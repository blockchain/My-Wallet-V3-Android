package info.blockchain.wallet.shapeshift.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.ParseException;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Trade {

    /*
    @STATUS
     */
    @JsonProperty("status")
    private String status;

    /*
    Tx hash (user tx hash to shapeshift)
     */
    @JsonProperty("hashIn")
    private String hashIn;

    /*
    Tx hash (shapeshift tx hash to user)
     */
    @JsonProperty("hashOut")
    private String hashOut;

    //Ignored - web-wallet js time string
//    @JsonProperty("time")
//    private String time;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("quote")
    private Quote quote;

    @Nullable
    public STATUS getStatus() {
        return STATUS.fromString(status);
    }

    public void setStatus(STATUS status) {
        this.status = status.toString();
    }

    public String getHashIn() {
        return hashIn;
    }

    public void setHashIn(String hashIn) {
        this.hashIn = hashIn;
    }

    @Nullable
    public String getHashOut() {
        return hashOut;
    }

    public void setHashOut(String hashOut) {
        this.hashOut = hashOut;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    /**
     * @return Coin type a acquired from trade
     */
    public String getAcquiredCoinType() {

        if(quote.getPair() != null &&
            quote.getPair().contains("_") &&
            quote.getPair().split("_").length > 1) {
            return quote.getPair().split("_")[1];
        } else {
            return "";
        }
    }

    public static Trade fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, Trade.class);
    }

    @JsonIgnore
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public enum STATUS {
        NO_DEPOSITS("no_deposits"),
        RECEIVED("received"),
        COMPLETE("complete"),
        FAILED("failed"),
        RESOLVED("resolved");

        private final String text;

        STATUS(final String text) {
            this.text = text;
        }

        static STATUS fromString(String text) {
            for (STATUS status : STATUS.values()) {
                if (status.text.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
