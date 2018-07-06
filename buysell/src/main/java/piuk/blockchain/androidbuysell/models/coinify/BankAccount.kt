package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.Json

data class BankAccount(
    val id: Int? = null,
    val account: Account,
    val bank: Bank,
    val holder: Holder,
    @Json(name = "update_time") val updateTime: String? = null,
    @Json(name = "create_time") val createTime: String? = null
)