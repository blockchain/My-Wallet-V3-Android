package piuk.blockchain.android.ui.buysell.createorder.models

enum class OrderType {
    // Unknown payment type, not yet selected
    Buy,
    // Bank only payment, trader must have been KYC'd
    BuyBank,
    // Card only payment, trader hasn't been KYC'd fully yet
    BuyCard,
    // Sell is bank payment only
    Sell
}