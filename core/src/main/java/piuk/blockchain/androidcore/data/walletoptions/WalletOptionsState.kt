package piuk.blockchain.androidcore.data.walletoptions

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.subjects.ReplaySubject
import piuk.blockchain.androidcore.utils.helperfunctions.InvalidatableLazy

class WalletOptionsState {

    private val optionsInitializer = InvalidatableLazy { ReplaySubject.create<WalletOptions>(1) }
    private val settingsInitializer = InvalidatableLazy { ReplaySubject.create<Settings>(1) }

    val walletOptionsSource: ReplaySubject<WalletOptions> by optionsInitializer
    val walletSettingsSource: ReplaySubject<Settings>by settingsInitializer

    fun wipe() {
        optionsInitializer.invalidate()
        settingsInitializer.invalidate()
    }
}
