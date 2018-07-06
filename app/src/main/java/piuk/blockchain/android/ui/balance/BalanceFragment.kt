package piuk.blockchain.android.ui.balance

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.android.synthetic.main.include_no_transaction_message.*
import kotlinx.android.synthetic.main.view_expanding_currency_header.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.websocket.WebSocketService
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.adapter.AccountsAdapter
import piuk.blockchain.android.ui.balance.adapter.TxFeedAdapter
import piuk.blockchain.android.ui.balance.adapter.TxFeedClickListener
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.onItemSelectedListener
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class BalanceFragment : BaseFragment<BalanceView, BalancePresenter>(), BalanceView,
    TxFeedClickListener {

    private var accountsAdapter: AccountsAdapter? = null
    private var txFeedAdapter: TxFeedAdapter? = null

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var balancePresenter: BalancePresenter

    private var interactionListener: OnFragmentInteractionListener? = null
    private var spacerDecoration: BottomSpacerDecoration? = null
    private var backPressed: Long = 0
    private val itemSelectedListener =
        onItemSelectedListener {
            currency_header?.close()
            presenter.onAccountSelected(it)
            recyclerview.smoothScrollToPosition(0)
        }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_INTENT && activity != null) {
                recyclerview?.scrollToPosition(0)
                presenter.onRefreshRequested()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_balance)

    override fun createPresenter() = balancePresenter

    override fun getMvpView() = this

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.apply {
            (activity as MainActivity).setOnTouchOutsideViewListener(app_bar,
                object : OnTouchOutsideViewListener {
                    override fun onTouchOutside(view: View, event: MotionEvent) {
                        currency_header.close()
                    }
                })
        }

        swipe_container.setProgressViewEndTarget(
            false,
            ViewUtils.convertDpToPixel(72F + 20F, context).toInt()
        )
        swipe_container.setOnRefreshListener { presenter.onRefreshRequested() }
        swipe_container.setColorSchemeResources(
            R.color.product_green_medium,
            R.color.primary_blue_medium,
            R.color.product_red_medium
        )

        textview_balance.setOnClickListener {
            presenter.onBalanceClick()
            currency_header?.close()
        }
        currency_header.setSelectionListener { presenter.onCurrencySelected(it) }

        onViewReady()

        presenter.onRefreshRequested()
    }

    override fun disableCurrencyHeader() {
        textview_selected_currency?.apply {
            isClickable = false
        }
    }

    fun refreshSelectedCurrency() {
        currency_header?.getCurrentlySelectedCurrency()?.run {
            if (presenter.getCurrentCurrency() != this) {
                presenter.onCurrencySelected(presenter.getCurrentCurrency())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
        }

        LocalBroadcastManager.getInstance(context!!)
            .registerReceiver(receiver, IntentFilter(ACTION_INTENT))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(receiver)
        // Fixes issue with Swipe Layout messing with Fragment transitions
        swipe_container?.let {
            swipe_container.isRefreshing = false
            swipe_container.destroyDrawingCache()
            swipe_container.clearAnimation()
        }

        currency_header?.close()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        interactionListener = activity as OnFragmentInteractionListener?
    }

    override fun shouldShowBuy() = AndroidUtils.is19orHigher()

    override fun setupAccountsAdapter(accountsList: List<ItemAccount>) {
        if (accountsAdapter == null) {
            accountsAdapter = AccountsAdapter(
                context,
                R.layout.spinner_balance_header,
                accountsList
            ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }
        }

        accounts_spinner.apply {
            adapter = accountsAdapter
            onItemSelectedListener = itemSelectedListener
            setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_UP && (activity as MainActivity).drawerOpen
            }
        }
    }

    override fun setupTxFeedAdapter(isCrypto: Boolean) {
        if (txFeedAdapter == null) {
            txFeedAdapter = TxFeedAdapter(activity!!, isCrypto, this)

            recyclerview.layoutManager = LayoutManager(context!!)
            recyclerview.adapter = txFeedAdapter
            // Disable blinking animations in RecyclerView
            val animator = recyclerview.itemAnimator
            if (animator is SimpleItemAnimator) animator.supportsChangeAnimations = false
        }
    }

    override fun selectDefaultAccount() {
        if (accountsAdapter?.isNotEmpty == true) {
            accounts_spinner.apply {
                onItemSelectedListener = null
                setSelection(0, false)
                onItemSelectedListener = itemSelectedListener
            }
        }
    }

    override fun updateAccountsDataSet(accountsList: List<ItemAccount>) {
        accountsAdapter?.updateAccountList(accountsList)
    }

    override fun updateTransactionDataSet(isCrypto: Boolean, displayObjects: List<Any>) {
        setupTxFeedAdapter(isCrypto)
        txFeedAdapter!!.items = displayObjects
        addBottomNavigationBarSpace()
    }

    /**
     * Adds space to bottom of tx feed recyclerview to make room for bottom navigation bar
     */
    private fun addBottomNavigationBarSpace() {
        if (spacerDecoration == null) {
            spacerDecoration = BottomSpacerDecoration(
                ViewUtils.convertDpToPixel(56f, context).toInt()
            )
        }
        recyclerview?.apply {
            removeItemDecoration(spacerDecoration)
            addItemDecoration(spacerDecoration)
        }
    }

    /**
     * Updates launcher shortcuts with latest receive address
     */
    @TargetApi(Build.VERSION_CODES.M)
    override fun generateLauncherShortcuts() {
        if (AndroidUtils.is25orHigher() && presenter.areLauncherShortcutsEnabled()) {
            val launcherShortcutHelper = LauncherShortcutHelper(
                activity,
                presenter.payloadDataManager,
                activity!!.getSystemService(ShortcutManager::class.java)
            )

            launcherShortcutHelper.generateReceiveShortcuts()
        }
    }

    override fun updateTransactionValueType(showCrypto: Boolean) {
        txFeedAdapter?.onViewFormatUpdated(showCrypto)
    }

    fun onBackPressed() {
        if (currency_header.isOpen()) {
            currency_header.close()
        } else {
            if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
                AccessState.getInstance().logout(context)
                return
            } else {
                toast(R.string.exit_confirm)
            }

            backPressed = System.currentTimeMillis()
        }
    }

    private fun setShowRefreshing(showRefreshing: Boolean) {
        swipe_container.isRefreshing = showRefreshing
    }

    override fun setDropdownVisibility(visible: Boolean) {
        layout_spinner.goneIf { !visible }
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.FAILURE, UiState.EMPTY -> onEmptyState()
            UiState.CONTENT -> onContentLoaded()
            UiState.LOADING -> {
                textview_balance.text = ""
                setShowRefreshing(true)
            }
        }
    }

    private fun onEmptyState() {
        setShowRefreshing(false)
        no_transaction_include.visible()

        when (presenter.getCurrentCurrency()) {
            CryptoCurrencies.BTC -> {
                button_get_bitcoin.setText(R.string.onboarding_get_bitcoin)
                button_get_bitcoin.setOnClickListener {
                    if (shouldShowBuy()) {
                        presenter.onGetBitcoinClicked()
                    } else {
                        startReceiveFragmentBtc()
                    }
                }
                description.setText(R.string.transaction_occur_when_bitcoin)
            }
            CryptoCurrencies.ETHER -> {
                button_get_bitcoin.setText(R.string.onboarding_get_eth)
                button_get_bitcoin.setOnClickListener { startReceiveFragmentEth() }
                description.setText(R.string.transaction_occur_when_eth)
            }
            CryptoCurrencies.BCH -> {
                button_get_bitcoin.setText(R.string.onboarding_get_bitcoin_cash)
                button_get_bitcoin.setOnClickListener { startReceiveFragmentBch() }
                description.setText(R.string.transaction_occur_when_bitcoin_cash)
            }
            else -> throw IllegalArgumentException(
                "Cryptocurrency ${presenter.getCurrentCurrency().unit} not supported"
            )
        }
    }

    override fun updateBalanceHeader(balance: String) {
        textview_balance.text = balance
    }

    private fun onContentLoaded() {
        setShowRefreshing(false)
        no_transaction_include.gone()
    }

    override fun startReceiveFragmentBtc() = broadcastIntent(MainActivity.ACTION_RECEIVE)

    private fun startReceiveFragmentEth() = broadcastIntent(MainActivity.ACTION_RECEIVE_ETH)

    private fun startReceiveFragmentBch() = broadcastIntent(MainActivity.ACTION_RECEIVE_BCH)

    override fun startBuyActivity() = broadcastIntent(MainActivity.ACTION_BUY)

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        val bundle = Bundle()
        bundle.putInt(KEY_TRANSACTION_LIST_POSITION, correctedPosition)
        TransactionDetailActivity.start(activity, bundle)
    }

    /*
    Toggle between fiat - crypto currency
     */
    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    override fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        currency_header?.setCurrentlySelectedCurrency(cryptoCurrency)
    }

    private fun broadcastIntent(action: String) {
        activity?.run {
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(action))
        }
    }

    override fun getCurrentAccountPosition() = accounts_spinner.selectedItemPosition

    companion object {

        const val ACTION_INTENT = WebSocketService.ACTION_INTENT
        const val KEY_TRANSACTION_LIST_POSITION = "transaction_list_position"
        const val KEY_TRANSACTION_HASH = "transaction_hash"

        private const val ARGUMENT_BROADCASTING_PAYMENT = "broadcasting_payment"
        private const val COOL_DOWN_MILLIS = 2 * 1000

        @JvmStatic
        fun newInstance(broadcastingPayment: Boolean): BalanceFragment {
            return BalanceFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARGUMENT_BROADCASTING_PAYMENT, broadcastingPayment)
                }
            }
        }
    }

    interface OnFragmentInteractionListener {
        fun resetNavigationDrawer()
    }

    private inner class LayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun supportsPredictiveItemAnimations() = false
    }
}