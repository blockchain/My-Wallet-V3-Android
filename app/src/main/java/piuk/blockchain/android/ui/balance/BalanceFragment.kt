package piuk.blockchain.android.ui.balance


import android.content.*
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatSpinner
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.android.synthetic.main.include_no_transaction_message.*
import kotlinx.android.synthetic.main.include_onboarding_complete.*
import kotlinx.android.synthetic.main.include_onboarding_viewpager.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.adapter.BalanceAdapter
import piuk.blockchain.android.ui.balance.adapter.BalanceListClickListener
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.onboarding.OnboardingPagerAdapter
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.ui.receive.ReceiveFragment
import piuk.blockchain.android.ui.send.SendFragment
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.*
import piuk.blockchain.android.util.helperfunctions.OnItemSelectedListener
import piuk.blockchain.android.util.helperfunctions.OnPageChangeListener
import javax.inject.Inject

class BalanceFragment : BaseFragment<BalanceView, BalancePresenter>(), BalanceView, BalanceListClickListener {

    override val isContactsEnabled: Boolean
        get() = BuildConfig.CONTACTS_ENABLED
    override val shouldShowBuy: Boolean
        get() = AndroidUtils.is19orHigher()

    @Inject lateinit var balancePresenter: BalancePresenter
    // Adapters
    private var accountsAdapter: BalanceHeaderAdapter? = null
    private var balanceAdapter: BalanceAdapter? = null
    private var onboardingPagerAdapter: OnboardingPagerAdapter? = null

    private var progressDialog: MaterialProgressDialog? = null
    private var interactionListener: OnFragmentInteractionListener? = null
    private var spacerDecoration: BottomSpacerDecoration? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_INTENT && activity != null) {
                onViewReady()
                recyclerview.scrollToPosition(0)
            }
        }
    }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = container!!.inflate(R.layout.fragment_balance)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        textview_balance.setOnClickListener { presenter.invertViewType() }
        button_get_bitcoin.setOnClickListener { presenter.getBitcoinClicked() }

        if (!presenter.isOnboardingComplete()) {
            initOnboardingPager()
        }
        setUiState(UiState.LOADING)
    }

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        goToTransactionDetail(correctedPosition)
    }

    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    override fun onFctxClicked(fctxId: String) {
        presenter.onPendingTransactionClicked(fctxId)
    }

    override fun onFctxLongClicked(fctxId: String) {
        presenter.onPendingTransactionLongClicked(fctxId)
    }

    override fun onViewTypeChanged(isBtc: Boolean, btcFormat: Int) {
        balanceAdapter?.onViewFormatUpdated(isBtc, btcFormat)
        accountsAdapter?.notifyBtcChanged(isBtc, btcFormat)
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.FAILURE, UiState.EMPTY -> onEmptyState()
            UiState.CONTENT -> onContentLoaded()
            UiState.LOADING -> setShowRefreshing(true)
        }
    }

    override fun onAccountsUpdated(
            accounts: List<ItemAccount>,
            lastPrice: Double,
            fiat: String,
            monetaryUtil: MonetaryUtil,
            isBtc: Boolean
    ) {
        if (accountsAdapter == null) {
            accountsAdapter = BalanceHeaderAdapter(
                    context,
                    R.layout.spinner_balance_header,
                    accounts,
                    isBtc,
                    monetaryUtil,
                    fiat,
                    lastPrice
            ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }

            accounts_spinner.adapter = accountsAdapter
        }

        if (accounts.size > 1) {
            accounts_spinner.visible()
        } else if (accounts.isNotEmpty()) {
            accounts_spinner.setSelection(0, false)
            accounts_spinner.invisible()
        }
        accounts_spinner.setOnTouchListener({ _, event ->
            event.action == MotionEvent.ACTION_UP && (activity as MainActivity).drawerOpen
        })

        accounts_spinner.onItemSelectedListener = OnItemSelectedListener {
            presenter.onAccountChosen(it)
            recyclerview.scrollToPosition(0)
        }
    }

    override fun onTotalBalanceUpdated(balance: String) {
        textview_balance.text = balance
    }

    override fun onTransactionsUpdated(displayObjects: List<Any>) {
        balanceAdapter?.items = displayObjects

        if (spacerDecoration == null) {
            spacerDecoration = BottomSpacerDecoration(
                    ViewUtils.convertDpToPixel(56f, context).toInt()
            )
        }
        recyclerview?.apply {
            removeItemDecoration(spacerDecoration)
            addItemDecoration(spacerDecoration)
        }

        generateLauncherShortcuts()
    }

    override fun onContactsHashMapUpdated(
            transactionDisplayMap: MutableMap<String, ContactTransactionDisplayModel>
    ) {
        balanceAdapter?.onContactsMapChanged(transactionDisplayMap)
    }

    override fun onExchangeRateUpdated(exchangeRate: Double, isBtc: Boolean) {
        if (balanceAdapter == null) {
            setUpRecyclerView(exchangeRate, isBtc)
        } else {
            balanceAdapter?.onPriceUpdated(exchangeRate)
        }
    }

    override fun onLoadOnboardingPages(pages: List<OnboardingPagerContent>) {
        if (onboardingPagerAdapter != null) {
            onboardingPagerAdapter!!.notifyPagesChanged(pages)

            pager_onboarding.post({
                progress_bar.gone()
            })

            indicator.setViewPager(pager_onboarding)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
        onViewReady()
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
        }

        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, IntentFilter(ACTION_INTENT))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        // Fixes issue with Swipe Layout messing with Fragment transitions
        swipe_container?.let {
            swipe_container.isRefreshing = false
            swipe_container.destroyDrawingCache()
            swipe_container.clearAnimation()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        interactionListener = activity as OnFragmentInteractionListener?
    }

    override fun showFctxRequiringAttention(number: Int) {
        activity?.let {
            (activity as MainActivity).setMessagesCount(number)
        }
    }

    override fun showToast(message: Int, toastType: String) = context.toast(message, toastType)

    override fun showPayOrDeclineDialog(fctxId: String, amount: String, name: String, note: String?) {
        val message: String
        if (!note.isNullOrEmpty()) {
            message = getString(R.string.contacts_balance_dialog_description_pr_note, name, amount, note)
        } else {
            message = getString(R.string.contacts_balance_dialog_description_pr_no_note, name, amount)
        }

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_balance_dialog_payment_requested)
                .setMessage(message)
                .setPositiveButton(
                        R.string.contacts_balance_dialog_accept,
                        { _, _ -> presenter.onPaymentRequestAccepted(fctxId) }
                )
                .setNegativeButton(
                        R.string.contacts_balance_dialog_decline,
                        { _, _ -> presenter.declineTransaction(fctxId) }
                )
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show()
    }

    override fun showSendAddressDialog(
            fctxId: String,
            amount: String,
            name: String,
            note: String?
    ) {
        val message: String
        if (!note.isNullOrEmpty()) {
            message = getString(R.string.contacts_balance_dialog_description_rpr_note, name, amount, note)
        } else {
            message = getString(R.string.contacts_balance_dialog_description_rpr_no_note, name, amount)
        }

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_balance_dialog_receiving_payment)
                .setMessage(message)
                .setPositiveButton(
                        R.string.contacts_balance_dialog_accept,
                        { _, _ -> presenter.onAccountChosen(0, fctxId) }
                )
                .setNegativeButton(
                        R.string.contacts_balance_dialog_decline,
                        { _, _ -> presenter.declineTransaction(fctxId) }
                )
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show()
    }

    override fun showWaitingForPaymentDialog() =
            showDialog(R.string.app_name, R.string.contacts_waiting_for_payment_message, null, false)

    override fun showWaitingForAddressDialog() =
            showDialog(R.string.app_name, R.string.contacts_waiting_for_address_message, null, false)

    override fun showTransactionDeclineDialog(fctxId: String) = showDialog(
            R.string.contacts_balance_dialog_decline_title,
            R.string.contacts_decline_pending_transaction,
            DialogInterface.OnClickListener { _, _ -> presenter.confirmDeclineTransaction(fctxId) },
            true
    )

    override fun showTransactionCancelDialog(fctxId: String) = showDialog(
            R.string.contacts_balance_dialog_cancel_title,
            R.string.contacts_cancel_pending_transaction,
            DialogInterface.OnClickListener { _, _ -> presenter.confirmCancelTransaction(fctxId) },
            true
    )

    override fun showAccountChoiceDialog(
            accounts: List<String>,
            fctxId: String,
            amount: String,
            name: String,
            note: String?
    ) {
        val spinner = AppCompatSpinner(activity)
        spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, accounts)
        val selection = intArrayOf(0)

        spinner.onItemSelectedListener = OnItemSelectedListener { selection[0] = it }

        var message: String
        if (!note.isNullOrEmpty()) {
            message = getString(R.string.contacts_balance_dialog_description_rpr_note, name, amount, note)
        } else {
            message = getString(R.string.contacts_balance_dialog_description_rpr_no_note, name, amount)
        }

        message += "\n\n${getString(R.string.contacts_balance_dialog_choose_account_message)}\n"

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_balance_dialog_receiving_payment)
                .setMessage(message)
                .setView(ViewUtils.getAlertDialogPaddedView(context, spinner))
                .setPositiveButton(
                        R.string.contacts_balance_dialog_accept,
                        { _, _ -> presenter.onAccountChosen(selection[0], fctxId) }
                )
                .setNegativeButton(
                        R.string.contacts_balance_dialog_decline,
                        { _, _ -> presenter.declineTransaction(fctxId) }
                )
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show()
    }

    override fun initiatePayment(uri: String, recipientId: String, mdid: String, fctxId: String) {
        interactionListener?.onPaymentInitiated(uri, recipientId, mdid, fctxId)
    }

    override fun startBuyActivity() {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(MainActivity.ACTION_BUY))
    }

    override fun startReceiveFragment() {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(MainActivity.ACTION_RECEIVE))
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(activity).apply {
            setCancelable(false)
            setMessage(R.string.please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun createPresenter() = balancePresenter

    override fun getMvpView() = this

    /**
     * Position is offset to account for first item being "All Wallets". If returned result is -1,
     * [SendFragment] and [ReceiveFragment] can safely ignore and choose the defaults
     * instead.
     */
    fun getSelectedAccountPosition(): Int {
        var position = accounts_spinner.selectedItemPosition
        if (position >= accounts_spinner.count - 1) {
            // End of list is imported addresses, ignore
            position = 0
        }

        return position - 1
    }

    private fun setShowRefreshing(showRefreshing: Boolean) {
        swipe_container.isRefreshing = showRefreshing
    }

    private fun onEmptyState() {
        setShowRefreshing(false)
        no_transaction_include.visible()

        if (!presenter.isOnboardingComplete()) {
            framelayout_onboarding.visible()
        } else {
            framelayout_onboarding.gone()
        }
    }

    private fun initOnboardingPager() {
        if (onboardingPagerAdapter == null) {
            onboardingPagerAdapter = OnboardingPagerAdapter(context)
            pager_onboarding.adapter = onboardingPagerAdapter
            pager_onboarding.addOnPageChangeListener(OnPageChangeListener { position, positionOffset ->
                val count = onboardingPagerAdapter?.count ?: 0
                if (position == count - 1) {
                    // Last page
                    onboarding_complete_layout.visible()
                    pager_onboarding.setPagingEnabled(false)
                    presenter.setOnboardingComplete(true)
                } else if (position == count - 2) {
                    // Second last page
                    onboarding_complete_layout.visible()
                    viewPagerIndicator.alpha = 1 - positionOffset
                    onboarding_complete_layout.alpha = positionOffset
                    presenter.setOnboardingComplete(false)
                } else {
                    viewPagerIndicator.visible()
                    onboarding_complete_layout.invisible()
                    viewPagerIndicator.alpha = 1.0f
                    presenter.setOnboardingComplete(false)
                }
            })
        }

        btn_skip_all.setOnClickListener { dismissOnboarding() }
        onboarding_close.setOnClickListener { dismissOnboarding() }

        button_start_over.setOnClickListener {
            onboarding_complete_layout.invisible()
            pager_onboarding.currentItem = 0
            pager_onboarding.setPagingEnabled(true)
            viewPagerIndicator.visible()
            viewPagerIndicator.alpha = 1.0f
            presenter.setOnboardingComplete(false)
        }
    }

    private fun dismissOnboarding() {
        framelayout_onboarding.gone()
        presenter.setOnboardingComplete(true)
        onboardingPagerAdapter = null
    }

    private fun onContentLoaded() {
        setShowRefreshing(false)
        no_transaction_include.gone()
    }

    private fun setUpRecyclerView(exchangeRate: Double, isBtc: Boolean) {
        balanceAdapter = BalanceAdapter(
                activity,
                exchangeRate,
                isBtc,
                this
        ).apply { setHasStableIds(true) }

        val layoutManager = LinearLayoutManager(context)
        recyclerview.layoutManager = layoutManager
        recyclerview.adapter = balanceAdapter
        // Disable blinking animations in RecyclerView
        val animator = recyclerview.itemAnimator
        if (animator is SimpleItemAnimator) animator.supportsChangeAnimations = false
    }

    private fun goToTransactionDetail(position: Int) {
        val bundle = Bundle()
        bundle.putInt(KEY_TRANSACTION_LIST_POSITION, position)
        TransactionDetailActivity.start(activity, bundle)
    }

    private fun generateLauncherShortcuts() {
        if (AndroidUtils.is25orHigher() && presenter.areLauncherShortcutsEnabled()) {
            val launcherShortcutHelper = LauncherShortcutHelper(
                    activity,
                    presenter.payloadDataManager,
                    activity.getSystemService(ShortcutManager::class.java))

            launcherShortcutHelper.generateReceiveShortcuts()
        }
    }

    private fun showDialog(
            @StringRes title: Int,
            message: String,
            clickListener: DialogInterface.OnClickListener?,
            showNegativeButton: Boolean
    ) {
        val builder = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setNegativeButton(android.R.string.cancel, null)

        if (showNegativeButton) {
            builder.setNegativeButton(android.R.string.cancel, null)
        }
        builder.show()
    }

    private fun showDialog(
            @StringRes title: Int,
            @StringRes message: Int,
            clickListener: DialogInterface.OnClickListener?,
            showNegativeButton: Boolean
    ) = showDialog(title, getString(message), clickListener, showNegativeButton)

    companion object {

        const val ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH"
        const val KEY_TRANSACTION_LIST_POSITION = "transaction_list_position"
        const val KEY_TRANSACTION_HASH = "transaction_hash"
        const val ARGUMENT_BROADCASTING_PAYMENT = "broadcasting_payment"

        @JvmStatic
        fun newInstance(broadcastingPayment: Boolean): BalanceFragment {
            val args = Bundle().apply { putBoolean(ARGUMENT_BROADCASTING_PAYMENT, broadcastingPayment) }
            return BalanceFragment().apply { arguments = args }
        }

    }

    interface OnFragmentInteractionListener {

        fun resetNavigationDrawer()

        fun onPaymentInitiated(uri: String, recipientId: String, mdid: String, fctxId: String)

    }

}