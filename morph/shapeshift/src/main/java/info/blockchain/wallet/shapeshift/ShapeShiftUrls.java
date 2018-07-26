package info.blockchain.wallet.shapeshift;

public final class ShapeShiftUrls {

    private ShapeShiftUrls() {
        throw new UnsupportedOperationException("You can't implement this class");
    }

    /* Base endpoint for all Shape Shift operations */
    public static final String SHAPESHIFT_URL = "https://shapeshift.io";

    /* Complete paths */
    static final String MARKET_INFO = SHAPESHIFT_URL + "/marketinfo";
    static final String SENDAMOUNT = SHAPESHIFT_URL + "/sendamount";
    static final String TX_STATS = SHAPESHIFT_URL + "/txStat";
    static final String TIME_REMAINING = SHAPESHIFT_URL + "/timeremaining";

}
