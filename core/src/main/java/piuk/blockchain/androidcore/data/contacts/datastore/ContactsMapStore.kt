package piuk.blockchain.androidcore.data.contacts.datastore

import piuk.blockchain.androidcore.data.contacts.models.ContactTransactionDisplayModel
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore

class ContactsMapStore : SimpleDataStore {

    /**
     * A [MutableMap] containing a [ContactTransactionDisplayModel] keyed to a Tx hash for convenient
     * display.
     */
    val displayMap = mutableMapOf<String, ContactTransactionDisplayModel>()

    override fun clearData() {
        displayMap.clear()
    }
}
