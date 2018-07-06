package piuk.blockchain.androidcoreui.utils.logging

import com.crashlytics.android.answers.CustomEvent
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcoreui.utils.extensions.getAmountRangeBch
import piuk.blockchain.androidcoreui.utils.extensions.getAmountRangeBtc
import piuk.blockchain.androidcoreui.utils.extensions.getAmountRangeEth
import java.math.BigInteger

class RecoverWalletEvent : CustomEvent("Recover Wallet") {

    fun putSuccess(successful: Boolean): RecoverWalletEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }
}

class PairingEvent : CustomEvent("Wallet Pairing") {

    fun putSuccess(successful: Boolean): PairingEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }

    fun putMethod(pairingMethod: PairingMethod): PairingEvent {
        putCustomAttribute("Pairing method", pairingMethod.name)
        return this
    }
}

@Suppress("UNUSED_PARAMETER")
enum class PairingMethod(name: String) {
    MANUAL("Manual"),
    QR_CODE("Qr code"),
    REVERSE("Reverse")
}

class PaymentSentEvent : CustomEvent("Payment Sent") {

    fun putSuccess(successful: Boolean): PaymentSentEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }

    fun putAmountForRange(
        amountSent: BigInteger,
        cryptoCurrencies: CryptoCurrencies
    ): PaymentSentEvent {
        val amountRange = when (cryptoCurrencies) {
            CryptoCurrencies.BTC -> amountSent.getAmountRangeBtc()
            CryptoCurrencies.ETHER -> amountSent.getAmountRangeEth()
            CryptoCurrencies.BCH -> amountSent.getAmountRangeBch()
        }

        putCustomAttribute("Amount", amountRange)
        return this
    }

    fun putCurrencyType(cryptoCurrencies: CryptoCurrencies): PaymentSentEvent {
        putCustomAttribute("Currency", cryptoCurrencies.symbol)
        return this
    }
}

class ImportEvent(addressType: AddressType) : CustomEvent("Address Imported") {

    init {
        putCustomAttribute("Address Type", addressType.name)
    }
}

@Suppress("UNUSED_PARAMETER")
enum class AddressType(name: String) {
    PRIVATE_KEY("Private key"),
    WATCH_ONLY("Watch Only")
}

class CreateAccountEvent(number: Int) : CustomEvent("Account Created") {

    init {
        putCustomAttribute("Number of Accounts", number)
    }
}

class AppLaunchEvent(playServicesFound: Boolean) : CustomEvent("App Launched") {

    init {
        putCustomAttribute("Play Services found", if (playServicesFound) "true" else "false")
    }
}

class SecondPasswordEvent(secondPasswordEnabled: Boolean) : CustomEvent("Second password event") {

    init {
        putCustomAttribute(
            "Second password enabled",
            if (secondPasswordEnabled) "true" else "false"
        )
    }
}

class ContactsEvent(eventType: ContactEventType) : CustomEvent("Contacts Event") {

    init {
        putCustomAttribute("Contacts event", eventType.name)
    }
}

@Suppress("UNUSED_PARAMETER")
enum class ContactEventType(name: String) {
    RPR("Request for payment request sent"),
    PR("Payment request sent"),
    PAYMENT_BROADCASTED("Payment broadcasted"),
    INVITE_SENT("Invite sent"),
    INVITE_ACCEPTED("Invite accepted")
}

class ShapeShiftEvent : CustomEvent("ShapeShift Used") {

    fun putPair(from: String, to: String): ShapeShiftEvent {
        putCustomAttribute("Currency Pair", "$from to $to")
        return this
    }

    fun putSuccess(successful: Boolean): ShapeShiftEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }
}

class LauncherShortcutEvent(type: String) : CustomEvent("Launcher Shortcut") {

    init {
        putCustomAttribute("Launcher Shortcut used", type)
    }
}

class WalletUpgradeEvent(successful: Boolean) : CustomEvent("Wallet Upgraded") {

    init {
        putCustomAttribute("Successful", if (successful) "true" else "false")
    }
}

class BalanceLoadedEvent(
    hasBtcBalance: Boolean,
    hasBchBalance: Boolean,
    hasEthBalance: Boolean
) : CustomEvent("Balances loaded") {

    init {
        putCustomAttribute("Has BTC balance", if (hasBtcBalance) "true" else "false")
        putCustomAttribute("Has BCH balance", if (hasBchBalance) "true" else "false")
        putCustomAttribute("Has ETH balance", if (hasEthBalance) "true" else "false")

        val hasAnyBalance = hasBtcBalance || hasBchBalance || hasEthBalance

        putCustomAttribute("Has any balance", if (hasAnyBalance) "true" else "false")
    }
}
