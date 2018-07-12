package piuk.blockchain.androidcore.data.shapeshift.datastore

import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple class for persisting ShapeShift Trade data.
 */
@Singleton
class ShapeShiftDataStore @Inject constructor() : SimpleDataStore {

    var tradeData: ShapeShiftTrades? = null

    override fun clearData() {
        tradeData = null
    }
}