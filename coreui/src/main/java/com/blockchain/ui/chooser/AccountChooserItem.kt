package com.blockchain.ui.chooser

import com.blockchain.serialization.JsonSerializableAccount

sealed class AccountChooserItem(val label: String) {
    class Header(
        label: String
    ) : AccountChooserItem(label)

    class Contact(
        label: String,
        val accountObject: JsonSerializableAccount?
    ) : AccountChooserItem(label)

    class AccountSummary(
        label: String,
        val displayBalance: String,
        val accountObject: JsonSerializableAccount?
    ) : AccountChooserItem(label)

    class LegacyAddress(
        label: String,
        val address: String?,
        val displayBalance: String,
        val isWatchOnly: Boolean,
        val accountObject: JsonSerializableAccount?
    ) : AccountChooserItem(label)
}
