package info.blockchain.wallet.ethereum

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.HashMap

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)

class Erc20TokenData {

    @JsonProperty("label")
    var label: String = ""

    @JsonProperty("contract")
    var contractAddress: String = ""
        private set

    @JsonProperty("has_seen")
    var hasSeen: Boolean = false

    @JsonProperty("tx_notes")
    val txNotes: HashMap<String, String> = HashMap()

    fun putTxNote(txHash: String, txNote: String) {
        txNotes[txHash] = txNote
    }

    fun removeTxNote(txHash: String) {
        txNotes.remove(txHash)
    }

    fun clearTxNotes() {
        txNotes.clear()
    }

    fun hasLabelAndAddressStored(): Boolean =
        contractAddress.isNotBlank() && label.isNotBlank()

    companion object {
        const val PAX_CONTRACT_NAME = "pax"
        const val USDT_CONTRACT_NAME = "usdt"
        const val DGLD_CONTRACT_NAME = "wdgld"
        const val AAVE_CONTRACT_NAME = "aave"
        const val YFI_CONTRACT_NAME = "yfi"
        private const val PAX_CONTRACT_ADDRESS = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        private const val USDT_CONTRACT_ADDRESS = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        private const val DGLD_CONTRACT_ADDRESS = "0x123151402076fc819b7564510989e475c9cd93ca"
        private const val AAVE_CONTRACT_ADDRESS = "0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9"
        private const val YFI_CONTRACT_ADDRESS = "0x0bc529c00C6401aEF6D220BE8C6Ea1667F6Ad93e"

        fun createPaxTokenData(label: String) = Erc20TokenData().apply {
            contractAddress = PAX_CONTRACT_ADDRESS
            this.label = label
        }

        fun createUsdtTokenData(label: String) = Erc20TokenData().apply {
            contractAddress = USDT_CONTRACT_ADDRESS
            this.label = label
        }

        fun createDgldTokenData(label: String) = Erc20TokenData().apply {
            contractAddress = DGLD_CONTRACT_ADDRESS
            this.label = label
        }

        fun createAaveTokenData(label: String) = Erc20TokenData().apply {
            contractAddress = AAVE_CONTRACT_ADDRESS
            this.label = label
        }

        fun createYfiTokenData(label: String) = Erc20TokenData().apply {
            contractAddress = YFI_CONTRACT_ADDRESS
            this.label = label
        }
    }
}