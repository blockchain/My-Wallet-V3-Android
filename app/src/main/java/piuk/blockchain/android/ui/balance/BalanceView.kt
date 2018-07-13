package piuk.blockchain.android.ui.balance

import piuk.blockchain.android.ui.account.ItemAccount
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.ui.base.View

interface BalanceView : View {

    fun setupAccountsAdapter(accountsList: List<ItemAccount>)

    fun setupTxFeedAdapter(isCrypto: Boolean)

    fun updateTransactionDataSet(isCrypto: Boolean, displayObjects: List<Any>)

    fun updateAccountsDataSet(accountsList: List<ItemAccount>)

    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun updateBalanceHeader(balance: String)

    fun selectDefaultAccount()

    fun setUiState(@UiState.UiStateDef uiState: Int)

    fun updateTransactionValueType(showCrypto: Boolean)

    fun startReceiveFragmentBtc()

    fun startBuyActivity()

    fun getCurrentAccountPosition(): Int?

    fun generateLauncherShortcuts()

    fun shouldShowBuy(): Boolean

    fun setDropdownVisibility(visible: Boolean)

    fun disableCurrencyHeader()
}