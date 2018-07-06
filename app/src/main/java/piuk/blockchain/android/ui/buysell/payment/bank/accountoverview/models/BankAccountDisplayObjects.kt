package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models

// Marker interface for UI objects
interface BankAccountDisplayable

class AddAccountButton : BankAccountDisplayable

data class BankAccountListObject(
    val bankAccountId: Int,
    val iban: String
) : BankAccountDisplayable