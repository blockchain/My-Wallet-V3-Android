package piuk.blockchain.android.ui.chooser

import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcoreui.ui.base.View

interface AccountChooserView : View {

    val accountMode: AccountMode

    val isContactsEnabled: Boolean

    fun updateUi(items: List<ItemAccount>)

    fun showNoContacts()
}
