package piuk.blockchain.android.ui.shapeshift.models

import android.annotation.SuppressLint
import android.os.Parcelable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import kotlinx.android.parcel.Parcelize
import info.blockchain.balance.CryptoCurrency
import java.math.BigDecimal
import java.math.BigInteger

@SuppressLint("ParcelCreator")
@Parcelize
data class ShapeShiftData(
    val orderId: String,
    var fromCurrency: CryptoCurrency,
    var toCurrency: CryptoCurrency,
    // The amount you're sending to ShapeShift
    var depositAmount: BigDecimal,
    // The address you're sending the funds to
    var depositAddress: String,
    // Your change address
    var changeAddress: String,
    // The amount you'll get back from ShapeShift
    var withdrawalAmount: BigDecimal,
    // The address to which you want to receive funds
    var withdrawalAddress: String,
    // The offered exchange rate
    var exchangeRate: BigDecimal,
    // The fee to send funds to ShapeShift, in Wei or Satoshis
    var transactionFee: BigInteger,
    // The mining fee that ShapeShift takes from your new funds, *NOT* in Wei or Satoshis as this is returned from the server
    var networkFee: BigDecimal,
    // An address for if the trade fails or if there's change
    var returnAddress: String,
    // xPub for finding account
    var xPub: String,
    // Epoch time, in milliseconds
    var expiration: Long,
    // Fee information
    var gasLimit: BigInteger,
    var gasPrice: BigInteger,
    var feePerKb: BigInteger
) : Parcelable

data class TradeProgressUiState(
    @StringRes val title: Int,
    @StringRes val message: Int,
    @DrawableRes val icon: Int,
    val showSteps: Boolean,
    val stepNumber: Int
)