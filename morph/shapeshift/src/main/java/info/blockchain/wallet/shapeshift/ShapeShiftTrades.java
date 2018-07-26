package info.blockchain.wallet.shapeshift;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.wallet.shapeshift.data.State;
import info.blockchain.wallet.shapeshift.data.Trade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShapeShiftTrades {

    public static final int METADATA_TYPE_EXTERNAL = 6;

    @JsonProperty("trades")
    private List<Trade> trades;
    @JsonProperty("USAState")
    private State state;

    public ShapeShiftTrades() {
        this.trades = new ArrayList<>();
    }

    /**
     * Loads existing trades from derived trades metadata node.
     *
     * @return Existing shapeshift trades or Null if no existing trades found.
     */
    public static ShapeShiftTrades load(String json) throws IOException {
        if (json != null) {
            return fromJson(json);
        } else {
            return null;
        }
    }

    public static ShapeShiftTrades fromJson(String json) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        return mapper.readValue(json, ShapeShiftTrades.class);
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public List<Trade> getTrades() {
        return trades;
    }

    @JsonProperty("USAState")
    public State getUsState() {
        return state;
    }

    public synchronized void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

    @JsonProperty("USAState")
    public void setUsState(State state) {
        this.state = state;
    }

}
