package info.blockchain.wallet.payload.data

fun Wallet.spendableLegacyAddressStrings() =
    legacyAddressList
        .filterNot { it.isArchived }
        .filterNot { it.isWatchOnly }
        .map { it.address }
        .toSet()
        .toList()

fun Wallet.allSpendableAccountsAndAddresses(): List<String> =
    (activeXpubs() + spendableLegacyAddressStrings())
        .toSet()
        .toList()

private fun Wallet.activeXpubs(): List<String> =
    hdWallets?.let { it[0].activeXpubs } ?: emptyList()
