package info.blockchain.wallet.util

import info.blockchain.wallet.bch.BchMainNetParams
import info.blockchain.wallet.bch.CashAddress
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Coin
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.uri.BitcoinURI
import org.bitcoinj.uri.BitcoinURIParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.Locale
import java.util.regex.Pattern
import javax.annotation.Nonnull

object FormatsUtil {
    private val ignoreCaseEthPattern = Pattern.compile("(?i)^(0x)?[0-9a-f]{40}$")
    private val lowerCaseEthPattern = Pattern.compile("^(0x)?[0-9a-f]{40}$")
    private val upperCaseEthPattern = Pattern.compile("^(0x)?[0-9A-F]{40}$")
    private const val FIELD_PAYMENT_REQUEST_URL = "r="

    @JvmStatic
    fun isBitcoinUri(s: String): Boolean {
        return try {
            BitcoinURI(s)
            true
        } catch (e: BitcoinURIParseException) {
            false
        }
    }

    @JvmStatic
    fun getPaymentRequestUrl(s: String): String {
        return if (s.contains(FIELD_PAYMENT_REQUEST_URL)) {
            val startPaymentUrlPosition = s.indexOf(FIELD_PAYMENT_REQUEST_URL) + FIELD_PAYMENT_REQUEST_URL.length
            return s.substring(startPaymentUrlPosition)
        } else {
            ""
        }
    }

    @JvmStatic @Nonnull
    fun getBitcoinAmount(s: String): String {
        return try {
            val uri = BitcoinURI(s)
            if (uri.amount != null) {
                uri.amount.toString()
            } else {
                "0.0000"
            }
        } catch (bupe: BitcoinURIParseException) {
            "0.0000"
        }
    }

    @JvmStatic
    fun isValidJson(json: String): Boolean {
        try {
            JSONObject(json)
        } catch (ex: JSONException) {
            try {
                JSONArray(json)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun isKeyEncrypted(data: String): Boolean {
        return if (isBase64(data)) {
            try {
                Base58.decode(data)
                false
            } catch (e: AddressFormatException) {
                true
            }
        } else {
            false
        }
    }

    @JvmStatic
    fun isKeyUnencrypted(data: String): Boolean {
        return try {
            Base58.decode(data)
            true
        } catch (e: AddressFormatException) {
            false
        }
    }

    private fun isBase64(data: String): Boolean {
        val regex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$"
        return data.matches(regex.toRegex()) && !containsSpaces(data)
    }

    private fun containsSpaces(data: String): Boolean {
        return data.contains(" ") ||
            data.contains("\n") ||
            data.contains("\r") ||
            data.contains("\t")
    }

    /**
     * Verify that a hex account string is a valid Ethereum address.
     *
     * @param address given address in HEX
     * @return is this a valid address
     */
    fun isValidEthereumAddress(address: String): Boolean {
        /*
         * check basic address requirements, i.e. is not empty and contains
         * the valid number and type of characters
         */
        return if (address.isEmpty() || !ignoreCaseEthPattern.matcher(address).find()) {
            false
        } else if (lowerCaseEthPattern.matcher(address).find() || upperCaseEthPattern.matcher(address).find()
        ) {
            // if it's all small caps or caps return true
            true
        } else {
            // if it is mixed caps it is a checksum address and needs to be validated
            validateChecksumEthereumAddress(address)
        }
    }

    fun isValidBitcoinAddress(
        address: String
    ) = isValidLegacyBtcAddress(address) || isValidBech32BtcAddress(address)

    private fun isValidLegacyBtcAddress(
        address: String
    ): Boolean = try {
        val networkParam = MainNetParams.get()
        LegacyAddress.fromBase58(networkParam, address)
        true
    } catch (ignored: AddressFormatException) {
        false
    }

    private fun isValidBech32BtcAddress(
        address: String
    ): Boolean = try {
        val networkParam = MainNetParams.get()
        SegwitAddress.fromBech32(networkParam, address)
        true
    } catch (ignored: AddressFormatException) {
        false
    }

    const val BTC_PREFIX = "bitcoin:"
    const val BCH_PREFIX = "bitcoincash:"
    const val ETHEREUM_PREFIX = "ethereum:"

    fun toDisambiguatedBtcAddress(
        address: String
    ): String =
        if (!address.startsWith(BTC_PREFIX)) {
            "$BTC_PREFIX$address"
        } else {
            address
        }

    @JvmStatic
    fun toBtcUri(
        address: String,
        amount: BigInteger = BigInteger.ZERO
    ): String {
        val v = if (amount.signum() == 1) Coin.valueOf(amount.toLong()) else null
        val a = when {
            isValidBech32BtcAddress(address) -> SegwitAddress.fromBech32(MainNetParams.get(), address)
            else -> LegacyAddress.fromBase58(MainNetParams.get(), address)
        }
        return BitcoinURI.convertToBitcoinURI(a, v, "", "")
    }

    /**
     * Verify that a String is a valid BECH32 Bitcoin Cash address.
     *
     * @param address The String you wish to test
     * @return Is this a valid BECH32 format BCH address
     */
    @JvmStatic
    fun isValidBCHAddress(address: String): Boolean {
        val networkParameters = BchMainNetParams.get()

        return if (address.isEmpty()) {
            false
        } else {
            try {
                CashAddress.decode(address)
                true
            } catch (e: AddressFormatException) {
                if (address.startsWith(networkParameters.segwitAddressHrp)) {
                    false
                } else {
                    isValidBCHAddress(
                        networkParameters.segwitAddressHrp +
                            BchMainNetParams.BECH32_SEPARATOR +
                            address
                    )
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun validateChecksumEthereumAddress(address: String): Boolean {
        val addr = address.replace("0x", "")
        val hash = Numeric.toHexStringNoPrefix(
            Hash.sha3(
                addr.toLowerCase(Locale.ROOT).toByteArray()
            )
        )
        for (i in 0..39) {
            if (Character.isLetter(addr[i])) {
                // each uppercase letter should correlate with a first bit of 1 in the hash
                // char with the same index, and each lowercase letter with a 0 bit
                val charInt = hash[i].toString().toInt(16)
                if (Character.isUpperCase(addr[i]) && charInt <= 7 || Character.isLowerCase(addr[i]) && charInt > 7) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Accepts bech32 cash address or base58 legacy address
     *
     * @return Short cash address (Example: qpmtetdtqpy5yhflnmmv8s35gkqfdnfdtywdqvue4p)
     */
    @JvmStatic fun toShortCashAddress(address: String): String {
        val networkParameters = BchMainNetParams.get()
        var addr = address
        if (addr.isEmpty()) {
            throw AddressFormatException("Invalid address format - $addr")
        }
        val result: String
        if (isValidBitcoinAddress(addr)) {
            addr = CashAddress.fromLegacyAddress(LegacyAddress.fromBase58(networkParameters, addr))
        }
        result = if (isValidBCHAddress(addr)) {
            addr.replace(
                networkParameters.segwitAddressHrp + BchMainNetParams.BECH32_SEPARATOR,
                ""
            )
        } else {
            throw AddressFormatException("Invalid address format - $address")
        }
        return result
    }

    fun makeFullBitcoinCashAddress(cashAddress: String): String {
        val networkParams = BchMainNetParams.get()
        return if (!cashAddress.startsWith(networkParams.segwitAddressHrp) &&
            isValidBCHAddress(cashAddress)
        ) {
            networkParams.segwitAddressHrp + BchMainNetParams.BECH32_SEPARATOR + cashAddress
        } else {
            cashAddress
        }
    }
}