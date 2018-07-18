package info.blockchain.wallet.payload.data

fun LegacyAddress.archive() {
    tag = LegacyAddress.ARCHIVED_ADDRESS
}

fun LegacyAddress.unarchive() {
    tag = LegacyAddress.NORMAL_ADDRESS
}

val LegacyAddress.isArchived: Boolean
    get() = tag == LegacyAddress.ARCHIVED_ADDRESS
