package piuk.blockchain.android.ui.chooser

import piuk.blockchain.androidcoreui.ui.base.View

interface AccountChooserView : View {

    val accountMode: AccountMode

    val isContactsEnabled: Boolean

    fun updateUi(items: List<AccountChooserItem>)

    fun showNoContacts()
}
