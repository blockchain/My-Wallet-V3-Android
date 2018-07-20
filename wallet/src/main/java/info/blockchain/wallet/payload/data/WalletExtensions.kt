package info.blockchain.wallet.payload.data

fun Wallet.nonArchivedLegacyAddressStrings() =
    nonArchivedLegacyAddresses()
        .addressSet()

fun Wallet.nonArchivedWatchOnlyLegacyAddressStrings() =
    nonArchivedLegacyAddresses()
        .filter { it.isWatchOnly }
        .addressSet()

fun Wallet.spendableLegacyAddressStrings() =
    nonArchivedLegacyAddresses()
        .filterNot { it.isWatchOnly }
        .addressSet()

fun Wallet.allSpendableAccountsAndAddresses() =
    activeXpubs() + spendableLegacyAddressStrings()

fun Wallet.allNonArchivedAccountsAndAddresses() =
    activeXpubs() + nonArchivedLegacyAddressStrings()

private fun Wallet.nonArchivedLegacyAddresses() =
    legacyAddressList
        .filterNot { it.isArchived }

private fun Iterable<LegacyAddress>.addressSet() =
    map { it.address }
        .toSet()

fun Wallet.activeXpubs() =
    hdWallets?.get(0)?.activeXpubs?.toSet() ?: emptySet()
