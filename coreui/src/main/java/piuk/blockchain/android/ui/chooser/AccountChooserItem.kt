package piuk.blockchain.android.ui.chooser

sealed class AccountChooserItem(val label: String) {
    class Header(
        label: String
    ) : AccountChooserItem(label)

    class Contact(
        label: String,
        val accountObject: Any?
    ) : AccountChooserItem(label)

    class AccountSummary(
        label: String,
        val displayBalance: String,
        val accountObject: Any?
    ) : AccountChooserItem(label)

    class LegacyAddress(
        label: String,
        val address: String?,
        val displayBalance: String,
        val isWatchOnly: Boolean,
        val accountObject: Any?
    ) : AccountChooserItem(label)
}
