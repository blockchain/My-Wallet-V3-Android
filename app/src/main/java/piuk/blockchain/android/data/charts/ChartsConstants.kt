package piuk.blockchain.android.data.charts

const val MARKET_PRICE = "market-price"
const val AVERAGE_8_HOURS = "8hours"

@Suppress("unused")
enum class TimeSpan(val timeValue: String) {
    YEAR("1year"),
    MONTH("4weeks"),
    WEEK("1week"),
    DAY("24hours")
}