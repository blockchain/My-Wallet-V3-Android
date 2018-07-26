package info.blockchain.wallet.shapeshift.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TimeRemaining {

    @JsonProperty("status")
    private final String status;
    @JsonProperty("seconds_remaining")
    private final Integer secondsRemaining;

    @JsonCreator
    public TimeRemaining(@JsonProperty("status") String status,
                         @JsonProperty("seconds_remaining") Integer secondsRemaining) {
        this.status = status;
        this.secondsRemaining = secondsRemaining;
    }

    /**
     * Returns the status of deposits to the address. Possible values are "pending" or "expired". If
     * expired, secondsRemaining will be 0. Note that an address can return as "pending" even if the
     * address has received funds.
     *
     * @return The current status of the address, whether "pending" or "expired"
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the number of seconds remaining to make a deposit to the address. Can be as much as
     * 600 (ten minutes). In testing, I've found that ShapeShift returns 1 and "pending" if the time
     * has expired but a payment was received.
     *
     * @return The number of seconds remaining.
     */
    public Integer getSecondsRemaining() {
        return secondsRemaining;
    }

}
