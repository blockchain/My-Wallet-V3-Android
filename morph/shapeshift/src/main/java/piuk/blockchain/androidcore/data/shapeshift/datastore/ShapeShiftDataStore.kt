package piuk.blockchain.androidcore.data.shapeshift.datastore

import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore

/**
 * A simple class for persisting ShapeShift Trade data.
 */
class ShapeShiftDataStore : SimpleDataStore {

    var tradeData: ShapeShiftTrades? = null

    override fun clearData() {
        tradeData = null
    }
}
