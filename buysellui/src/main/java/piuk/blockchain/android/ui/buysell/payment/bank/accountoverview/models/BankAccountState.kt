package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models

sealed class BankAccountState {

    object Loading : BankAccountState()
    object Failure : BankAccountState()
    data class Data(val displayData: List<BankAccountDisplayable>) : BankAccountState()
    object DeleteAccountFailure : BankAccountState()
}