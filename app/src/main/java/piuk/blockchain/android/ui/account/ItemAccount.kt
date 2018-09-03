package piuk.blockchain.android.ui.account

import com.blockchain.serialization.JsonSerializableAccount

@Suppress("LeakingThis")
class ItemAccount {

    enum class TYPE {
        ALL_ACCOUNTS_AND_LEGACY, ALL_LEGACY, SINGLE_ACCOUNT
    }

    var label: String? = null
    var displayBalance: String? = null
    var tag: String? = null
    var absoluteBalance: Long? = null

    // Ultimately this is used to sign txs
    var accountObject: JsonSerializableAccount? = null

    // Address/Xpub to fetch balance/tx list
    var address: String? = null
    var type: TYPE = TYPE.SINGLE_ACCOUNT

    constructor() {
        // Empty constructor for serialization
    }

    override fun toString(): String {
        return "ItemAccount(label=$label, " +
            "displayBalance=$displayBalance, " +
            "tag=$tag, " +
            "absoluteBalance=$absoluteBalance, " +
            "accountObject=$accountObject, " +
            "address=$address, " +
            "type=$type)"
    }

    @JvmOverloads
    constructor(
        label: String?,
        displayBalance: String?,
        tag: String?,
        absoluteBalance: Long?,
        accountObject: JsonSerializableAccount? = null,
        address: String?,
        type: TYPE = TYPE.SINGLE_ACCOUNT
    ) {
        this.label = label
        this.displayBalance = displayBalance
        this.tag = tag
        this.absoluteBalance = absoluteBalance
        this.address = address
        this.accountObject = accountObject
        this.type = type
    }

    constructor(label: String?) {
        this.label = label
    }
}
