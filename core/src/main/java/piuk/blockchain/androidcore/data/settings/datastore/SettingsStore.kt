package piuk.blockchain.androidcore.data.settings.datastore

import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import piuk.blockchain.androidcore.utils.Optional

interface SettingsStore {

    fun getSettings(): Observable<Optional<Settings>>
}