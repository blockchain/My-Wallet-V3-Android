package piuk.blockchain.android.ui.chooser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.android.synthetic.main.activity_account_chooser.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

class AccountChooserActivity : BaseMvpActivity<AccountChooserView, AccountChooserPresenter>(),
    AccountChooserView {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var accountChooserPresenter: AccountChooserPresenter

    override val isContactsEnabled: Boolean = BuildConfig.CONTACTS_ENABLED
    override val accountMode: AccountMode by unsafeLazy {
        intent.getSerializableExtra(EXTRA_CHOOSER_MODE) as AccountMode
    }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_chooser)

        require(intent.hasExtra(EXTRA_CHOOSER_MODE)) { "Chooser mode must be passed to AccountChooserActivity" }
        require(intent.hasExtra(EXTRA_ACTIVITY_TITLE)) { "Title string must be passed to AccountChooserActivity" }

        val title = intent.getStringExtra(EXTRA_ACTIVITY_TITLE)
        setupToolbar(toolbar_general, title)

        setSupportActionBar(toolbar_general)
        supportActionBar?.run { setDisplayHomeAsUpEnabled(true) }

        onViewReady()
    }

    override fun showNoContacts() {
        recyclerview.gone()
        layout_no_contacts.visible()
    }

    override fun onSupportNavigateUp(): Boolean =
        consume {
            setResult(Activity.RESULT_CANCELED)
            onBackPressed()
        }

    override fun updateUi(items: List<ItemAccount>) {
        val adapter = AccountChooserAdapter(items) { any ->
            if (any != null) {
                try {
                    val intent = Intent().apply {
                        putExtra(EXTRA_SELECTED_ITEM, any.toSerialisedString())
                        putExtra(EXTRA_SELECTED_OBJECT_TYPE, any.javaClass.name)
                    }

                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
            }
        }
        recyclerview.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(this@AccountChooserActivity)
        }
    }

    override fun createPresenter() = accountChooserPresenter

    override fun getView() = this

    companion object {

        private const val EXTRA_REQUEST_CODE = "piuk.blockchain.android.EXTRA_REQUEST_CODE"
        private const val EXTRA_CHOOSER_MODE = "piuk.blockchain.android.EXTRA_CHOOSER_MODE"

        const val EXTRA_SELECTED_ITEM = "piuk.blockchain.android.EXTRA_SELECTED_ITEM"
        const val EXTRA_SELECTED_OBJECT_TYPE = "piuk.blockchain.android.EXTRA_SELECTED_OBJECT_TYPE"
        const val EXTRA_ACTIVITY_TITLE = "piuk.blockchain.android.EXTRA_ACTIVITY_TITLE"

        fun startForResult(
            fragment: Fragment,
            accountMode: AccountMode,
            requestCode: Int,
            title: String
        ) {
            val starter = createIntent(fragment.context!!, accountMode, requestCode, title)
            fragment.startActivityForResult(starter, requestCode)
        }

        fun startForResult(
            activity: Activity,
            accountMode: AccountMode,
            requestCode: Int,
            title: String
        ) {
            val starter = createIntent(activity, accountMode, requestCode, title)
            activity.startActivityForResult(starter, requestCode)
        }

        private fun createIntent(
            context: Context,
            accountMode: AccountMode,
            requestCode: Int,
            title: String
        ): Intent = Intent(context, AccountChooserActivity::class.java).apply {
            putExtra(EXTRA_CHOOSER_MODE, accountMode)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
            putExtra(EXTRA_ACTIVITY_TITLE, title)
        }
    }
}

enum class AccountMode {

    // Show all accounts for ShapeShift, ie BTC & BCH HD accounts, Ether
    ShapeShift,
    // Show only the contacts list
    ContactsOnly,
    // Show all bitcoin accounts, including HD + legacy addresses
    Bitcoin,
    // Show all bitcoin accounts, no legacy addresses
    BitcoinHdOnly,
    // Show all bitcoin cash HD accounts, but no legacy addresses
    BitcoinCash,
    // Show all bitcoin cash HD accounts + all legacy addresses with balances + headers
    BitcoinCashSend
}
