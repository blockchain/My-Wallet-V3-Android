package info.blockchain.wallet.shapeshift

object ShapeShiftUrls {
    /* Base endpoint for all Shape Shift operations */
    const val SHAPESHIFT_URL = "https://shapeshift.io"

    /* Complete paths */
    internal const val MARKET_INFO = "$SHAPESHIFT_URL/marketinfo"
    internal const val SENDAMOUNT = "$SHAPESHIFT_URL/sendamount"
    internal const val TX_STATS = "$SHAPESHIFT_URL/txStat"
    internal const val TIME_REMAINING = "$SHAPESHIFT_URL/timeremaining"
}
