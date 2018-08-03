package piuk.blockchain.android.ui.chooser

enum class AccountMode {

    /**
     * Show all accounts for ShapeShift, ie BTC & BCH HD accounts, Ether
     */
    Exchange,

    /**
     * Show only the contacts list
     */
    ContactsOnly,

    /**
     * Show all bitcoin accounts, including HD + legacy addresses
     */
    Bitcoin,

    /**
     * Show all bitcoin accounts, no legacy addresses
     */
    BitcoinHdOnly,

    /**
     * Show all bitcoin cash HD accounts, but no legacy addresses
     */
    BitcoinCash,

    /**
     * Show all bitcoin cash HD accounts + all legacy addresses with balances + headers
     */
    BitcoinCashSend
}
