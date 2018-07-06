package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview

import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountState
import piuk.blockchain.androidcoreui.ui.base.View

interface BankAccountSelectionView : View {

    fun renderUiState(uiState: BankAccountState)
}
