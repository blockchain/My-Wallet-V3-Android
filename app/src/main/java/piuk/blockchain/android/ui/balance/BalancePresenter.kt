package piuk.blockchain.android.ui.balance

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.contacts.ContactsDataManager
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel
import piuk.blockchain.android.data.contacts.models.ContactsEvent
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.notifications.models.NotificationPayload
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

class BalancePresenter @Inject constructor(
        private val exchangeRateFactory: ExchangeRateFactory,
        private val transactionListDataManager: TransactionListDataManager,
        private val contactsDataManager: ContactsDataManager,
        private val swipeToReceiveHelper: SwipeToReceiveHelper,
        internal val payloadDataManager: PayloadDataManager,
        private val buyDataManager: BuyDataManager,
        private val stringUtils: StringUtils,
        private val prefsUtil: PrefsUtil,
        private val accessState: AccessState,
        private val rxBus: RxBus,
        private val appUtil: AppUtil
) : BasePresenter<BalanceView>() {

    @VisibleForTesting var contactsEventObservable: Observable<ContactsEvent>? = null
    @VisibleForTesting var notificationObservable: Observable<NotificationPayload>? = null
    @VisibleForTesting var authEventObservable: Observable<AuthEvent>? = null
    @VisibleForTesting var chosenAccount: ItemAccount? = null

    @VisibleForTesting internal val activeAccountAndAddressList: MutableList<ItemAccount> = mutableListOf()
    private val displayList: MutableList<Any> = mutableListOf()
    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    @SuppressLint("VisibleForTests")
    override fun onViewReady() {
        subscribeToEvents()
        storeSwipeReceiveAddresses()

        activeAccountAndAddressList.clear()
        activeAccountAndAddressList.addAll(getAllDisplayableAccounts())
        chosenAccount = activeAccountAndAddressList[0]

        chosenAccount?.let {
            Observable.merge(
                    getBalanceObservable(it),
                    getTransactionsListObservable(it),
                    getUpdateTickerObservable(),
                    getFacilitatedTransactionsObservable()
            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnError { Timber.e(it) }
                    .subscribe(
                            { /* No-op */ },
                            { view.setUiState(UiState.FAILURE) })
        }
    }

    override fun onViewDestroyed() {
        contactsEventObservable?.let { rxBus.unregister(ContactsEvent::class.java, it) }
        notificationObservable?.let { rxBus.unregister(NotificationPayload::class.java, it) }
        authEventObservable?.let { rxBus.unregister(AuthEvent::class.java, it) }
        super.onViewDestroyed()
    }

    internal fun onResume() {
        // Here we check the Fiat and Btc formats and let the UI handle any potential updates
        val btcUnitType = getBtcUnitType()
        monetaryUtil.updateUnit(btcUnitType)
        view.onExchangeRateUpdated(getLastPrice(getFiatCurrency()), accessState.isBtc)
        view.onViewTypeChanged(accessState.isBtc, btcUnitType)
    }

    internal fun onAccountChosen(position: Int) {
        chosenAccount = activeAccountAndAddressList[position]
        chosenAccount?.let {
            Observable.merge(
                    getBalanceObservable(it),
                    getTransactionsListObservable(it)
            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnError { Timber.e(it) }
                    .subscribe(
                            { /* No-op */ },
                            { view.setUiState(UiState.FAILURE) })
        }
    }

    internal fun onRefreshRequested() {
        chosenAccount?.let {
            Observable.merge(
                    getBalanceObservable(it),
                    getTransactionsListObservable(it),
                    getFacilitatedTransactionsObservable()
            ).compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnError { Timber.e(it) }
                    .subscribe(
                            { /* No-op */ },
                            { view.setUiState(UiState.FAILURE) })
        }
    }

    internal fun setViewType(isBtc: Boolean) {
        accessState.setIsBtc(isBtc)
        view.onViewTypeChanged(isBtc, getBtcUnitType())
        view.onTotalBalanceUpdated(getBalanceString(isBtc, chosenAccount?.absoluteBalance ?: 0L))
    }

    internal fun invertViewType() = setViewType(!accessState.isBtc)

    internal fun areLauncherShortcutsEnabled() =
            prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)

    internal fun onPendingTransactionClicked(fctxId: String) {
        contactsDataManager.getContactFromFctxId(fctxId)
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .subscribe({
                    val transaction = it.facilitatedTransactions[fctxId]

                    if (transaction == null) {
                        view.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)
                    } else {
                        when {
                            transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
                                    && transaction.role == FacilitatedTransaction.ROLE_RPR_INITIATOR ->
                                // Payment request sent, waiting for address from recipient
                                view.showWaitingForAddressDialog()

                            transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                                    && transaction.role == FacilitatedTransaction.ROLE_PR_INITIATOR ->
                                // Payment request sent, waiting for payment
                                view.showWaitingForPaymentDialog()

                            transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
                                    && transaction.role == FacilitatedTransaction.ROLE_RPR_RECEIVER ->
                                // Received payment request, need to send address to sender
                                showSendAddressDialog(
                                        fctxId,
                                        transaction.intendedAmount,
                                        it.name,
                                        transaction.note
                                )

                            transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                                    && transaction.role == FacilitatedTransaction.ROLE_PR_RECEIVER ->
                                // Waiting for payment, pay or reject
                                view.showPayOrDeclineDialog(
                                        fctxId,
                                        getBalanceString(true, transaction.intendedAmount),
                                        it.name,
                                        transaction.note
                                )

                            transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                                    && transaction.role == FacilitatedTransaction.ROLE_RPR_INITIATOR ->
                                // Need to send payment to recipient
                                view.initiatePayment(
                                        transaction.toBitcoinURI(),
                                        it.id,
                                        it.mdid,
                                        transaction.id
                                )

                            else -> view.showTransactionCancelDialog(fctxId)
                        }
                    }
                }, {
                    Timber.e(it)
                    view.showToast(
                            R.string.contacts_not_found_error,
                            ToastCustom.TYPE_ERROR
                    )
                })
    }

    internal fun onPaymentRequestAccepted(fctxId: String) {
        contactsDataManager.getContactFromFctxId(fctxId)
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .subscribe({
                    val transaction = it.facilitatedTransactions[fctxId]
                    if (transaction == null) {
                        view.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)
                    } else {
                        // Need to send payment to recipient
                        view.initiatePayment(
                                transaction.toBitcoinURI(),
                                it.id,
                                it.mdid,
                                transaction.id
                        )
                    }
                }, {
                    Timber.e(it)
                    view.showToast(
                            R.string.contacts_not_found_error,
                            ToastCustom.TYPE_ERROR
                    )
                })
    }

    internal fun onPendingTransactionLongClicked(fctxId: String) {
        contactsDataManager.getFacilitatedTransactions()
                .filter { it.facilitatedTransaction.id == fctxId }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({
                    val fctx = it.facilitatedTransaction

                    if (fctx.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
                            && fctx.role == FacilitatedTransaction.ROLE_RPR_INITIATOR) {
                        view.showTransactionCancelDialog(fctxId)
                    } else if (fctx.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                            && fctx.role == FacilitatedTransaction.ROLE_PR_INITIATOR) {
                        view.showTransactionCancelDialog(fctxId)
                    }
                }, { Timber.e(it) })
    }

    internal fun onAccountChosen(accountPosition: Int, fctxId: String) {
        contactsDataManager.getContactFromFctxId(fctxId)
                .doOnSubscribe { view.showProgressDialog() }
                .doOnError { view.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR) }
                .flatMapCompletable { contact ->
                    val transaction = contact.facilitatedTransactions[fctxId]

                    val paymentRequest = PaymentRequest()
                    paymentRequest.intendedAmount = transaction?.intendedAmount ?: 0L
                    paymentRequest.id = fctxId

                    payloadDataManager.getNextReceiveAddressAndReserve(
                            payloadDataManager.getPositionOfAccountInActiveList(accountPosition),
                            "Payment request ${transaction?.id}"
                    ).doOnNext { paymentRequest.address = it }
                            .flatMapCompletable {
                                contactsDataManager.sendPaymentRequestResponse(
                                        contact.mdid,
                                        paymentRequest,
                                        fctxId
                                )
                            }
                }
                .doAfterTerminate { view.dismissProgressDialog() }
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .subscribe(
                        {
                            view.showToast(R.string.contacts_address_sent_success, ToastCustom.TYPE_OK)
                            refreshFacilitatedTransactions()
                        },
                        { view.showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR) })
    }


    internal fun declineTransaction(fctxId: String) = view.showTransactionDeclineDialog(fctxId)

    internal fun confirmDeclineTransaction(fctxId: String) {
        contactsDataManager.getContactFromFctxId(fctxId)
                .flatMapCompletable { contactsDataManager.sendPaymentDeclinedResponse(it.mdid, fctxId) }
                .doOnError { contactsDataManager.fetchContacts() }
                .doAfterTerminate(this::refreshFacilitatedTransactions)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .subscribe(
                        { view.showToast(R.string.contacts_pending_transaction_decline_success, ToastCustom.TYPE_OK) },
                        { view.showToast(R.string.contacts_pending_transaction_decline_failure, ToastCustom.TYPE_ERROR) })
    }

    internal fun confirmCancelTransaction(fctxId: String) {
        contactsDataManager.getContactFromFctxId(fctxId)
                .flatMapCompletable { contactsDataManager.sendPaymentCancelledResponse(it.mdid, fctxId) }
                .doOnError { contactsDataManager.fetchContacts() }
                .doAfterTerminate(this::refreshFacilitatedTransactions)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError { Timber.e(it) }
                .subscribe(
                        { view.showToast(R.string.contacts_pending_transaction_cancel_success, ToastCustom.TYPE_OK) },
                        { view.showToast(R.string.contacts_pending_transaction_cancel_failure, ToastCustom.TYPE_ERROR) })
    }

    internal fun isOnboardingComplete() =
            // If wallet isn't newly created, don't show onboarding
            prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false) || !appUtil.isNewlyCreated

    internal fun setOnboardingComplete(completed: Boolean) {
        prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, completed)
    }

    internal fun getBitcoinClicked() {
        if (view.shouldShowBuy) {
            buyDataManager.canBuy
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe({
                        if (it) {
                            view.startBuyActivity()
                        } else {
                            view.startReceiveFragment()
                        }
                    }, { Timber.e(it) })
        } else {
            view.startReceiveFragment()
        }
    }

    internal fun disableAnnouncement() {
        prefsUtil.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
    }

    @VisibleForTesting
    internal fun getAllDisplayableAccounts(): MutableList<ItemAccount> {
        val mutableList = mutableListOf<ItemAccount>()

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        val accounts = payloadDataManager.accounts
                .filter { !it.isArchived }
                .map {
                    val bigIntBalance = payloadDataManager.getAddressBalance(it.xpub)
                    ItemAccount().apply {
                        label = it.label
                        displayBalance = getBalanceString(accessState.isBtc, bigIntBalance.toLong())
                        absoluteBalance = bigIntBalance.toLong()
                        address = it.xpub
                        type = ItemAccount.TYPE.SINGLE_ACCOUNT
                    }
                }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = payloadDataManager.walletBalance

            mutableList.add(ItemAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                displayBalance = getBalanceString(accessState.isBtc, bigIntBalance.toLong())
                absoluteBalance = bigIntBalance.toLong()
                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
            })
        }

        mutableList.addAll(accounts)

        // Show "Imported Addresses" if wallet contains legacy addresses
        if (!legacyAddresses.isEmpty()) {
            val bigIntBalance = payloadDataManager.importedAddressesBalance

            mutableList.add(ItemAccount().apply {
                displayBalance = getBalanceString(accessState.isBtc, bigIntBalance.toLong())
                label = stringUtils.getString(R.string.imported_addresses)
                absoluteBalance = bigIntBalance.toLong()
                type = ItemAccount.TYPE.ALL_LEGACY
            })
        }

        return mutableList
    }

    private fun showSendAddressDialog(fctxId: String, amount: Long, name: String, note: String?) {
        val accountNames = payloadDataManager.accounts
                .filterNot { it.isArchived }
                .mapTo(ArrayList<String>()) { it.label }

        if (accountNames.size == 1) {
            // Only one account, ask if you want to send an address
            view.showSendAddressDialog(
                    fctxId,
                    getBalanceString(true, amount),
                    name,
                    note
            )
        } else {
            // Show dialog allowing user to select which account they want to use
            view.showAccountChoiceDialog(
                    accountNames,
                    fctxId,
                    getBalanceString(true, amount),
                    name,
                    note
            )
        }
    }

    private fun getTransactionsListObservable(itemAccount: ItemAccount) =
            transactionListDataManager.fetchTransactions(itemAccount, 50, 0)
                    .doAfterTerminate(this::storeSwipeReceiveAddresses)
                    .doOnNext {
                        displayList.removeAll { it is TransactionSummary }
                        displayList.addAll(it)
                        checkLatestAnnouncement(displayList)

                        when {
                            displayList.isEmpty() -> view.setUiState(UiState.EMPTY)
                            else -> view.setUiState(UiState.CONTENT)
                        }
                        view.onTransactionsUpdated(displayList)
                    }

    private fun getBalanceObservable(itemAccount: ItemAccount) =
            payloadDataManager.updateAllBalances()
                    .doOnComplete {
                        val btcBalance = transactionListDataManager.getBtcBalance(itemAccount)
                        val balanceTotal = getBalanceString(accessState.isBtc, btcBalance)
                        view.onTotalBalanceUpdated(balanceTotal)
                    }.toObservable<Nothing>()

    private fun getUpdateTickerObservable() =
            exchangeRateFactory.updateTicker()
                    .doOnComplete {
                        view.onAccountsUpdated(
                                activeAccountAndAddressList,
                                getLastPrice(getFiatCurrency()),
                                getFiatCurrency(),
                                monetaryUtil,
                                accessState.isBtc
                        )
                        view.onExchangeRateUpdated(
                                exchangeRateFactory.getLastPrice(getFiatCurrency()),
                                accessState.isBtc
                        )
                    }.andThen(getOnboardingStatusObservable())

    private fun getOnboardingStatusObservable() = buyDataManager.canBuy
            .compose(RxUtil.addObservableToCompositeDisposable(this))
            .doOnNext { view.onLoadOnboardingPages(getOnboardingPages(it)) }
            .doOnError { Timber.e(it) }

    private fun getFacilitatedTransactionsObservable() = if (view.isContactsEnabled) {
        contactsDataManager.fetchContacts()
                .andThen(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .toList()
                .flatMapObservable { contactsDataManager.refreshFacilitatedTransactions() }
                .toList()
                .onErrorReturnItem(emptyList())
                .toObservable()
                .doOnNext {
                    handlePendingTransactions(it)
                    view.onContactsHashMapUpdated(contactsDataManager.getTransactionDisplayMap())
                }
    } else {
        Observable.empty()
    }

    private fun refreshFacilitatedTransactions() {
        getFacilitatedTransactionsObservable()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) })
    }

    private fun storeSwipeReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable {
            swipeToReceiveHelper.updateAndStoreAddresses()
            Void.TYPE
        }.subscribeOn(Schedulers.computation())
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) })
    }

    private fun subscribeToEvents() {
        contactsEventObservable = rxBus.register(ContactsEvent::class.java)
        contactsEventObservable?.subscribe({ refreshFacilitatedTransactions() })

        authEventObservable = rxBus.register(AuthEvent::class.java)
        authEventObservable?.subscribe({
            displayList.clear()
            transactionListDataManager.clearTransactionList()
            contactsDataManager.resetContacts()
        })

        notificationObservable = rxBus.register(NotificationPayload::class.java)
        notificationObservable?.subscribe({ notificationPayload ->
            if (notificationPayload.type != null
                    && notificationPayload.type == NotificationPayload.NotificationType.PAYMENT) {
                refreshFacilitatedTransactions()
            }
        })
    }

    private fun handlePendingTransactions(transactions: List<ContactTransactionModel>) {
        displayList.removeAll { it !is TransactionSummary }
        view.showFctxRequiringAttention(getNumberOfFctxRequiringAttention(transactions))
        if (transactions.isNotEmpty()) {
            val reversed = transactions.sortedBy { it.facilitatedTransaction.lastUpdated }.reversed()
            displayList.add(0, stringUtils.getString(R.string.contacts_pending_transaction))
            displayList.addAll(1, reversed)
            displayList.add(reversed.size + 1, stringUtils.getString(R.string.contacts_transaction_history))
            view.onTransactionsUpdated(displayList)
            view.setUiState(UiState.CONTENT)
        } else {
            view.onTransactionsUpdated(displayList)
        }
    }

    private fun getNumberOfFctxRequiringAttention(facilitatedTransactions: List<ContactTransactionModel>): Int {
        var value = 0
        facilitatedTransactions
                .asSequence()
                .map { it.facilitatedTransaction }
                .forEach {
                    if (it.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
                            && it.role == FacilitatedTransaction.ROLE_RPR_RECEIVER) {
                        value++
                    } else if (it.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
                            && (it.role == FacilitatedTransaction.ROLE_RPR_INITIATOR
                            || it.role == FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                        value++
                    }
                }
        return value
    }

    private fun getOnboardingPages(isBuyAllowed: Boolean): List<OnboardingPagerContent> {
        val pages = mutableListOf<OnboardingPagerContent>()
        if (isBuyAllowed) {
            // Buy bitcoin prompt
            pages.add(
                    OnboardingPagerContent(
                            stringUtils.getString(R.string.onboarding_current_price),
                            getFormattedPriceString(),
                            stringUtils.getString(R.string.onboarding_buy_content),
                            stringUtils.getString(R.string.onboarding_buy_bitcoin),
                            MainActivity.ACTION_BUY,
                            R.color.primary_blue_accent,
                            R.drawable.vector_buy_offset
                    ))
        }

        // Receive bitcoin
        pages.add(
                OnboardingPagerContent(
                        stringUtils.getString(R.string.onboarding_receive_bitcoin),
                        "",
                        stringUtils.getString(R.string.onboarding_receive_content),
                        stringUtils.getString(R.string.receive_bitcoin),
                        MainActivity.ACTION_RECEIVE,
                        R.color.secondary_teal_medium,
                        R.drawable.vector_receive_offset
                ))

        // QR Codes
        pages.add(
                OnboardingPagerContent(
                        stringUtils.getString(R.string.onboarding_qr_codes),
                        "",
                        stringUtils.getString(R.string.onboarding_qr_codes_content),
                        stringUtils.getString(R.string.onboarding_scan_address),
                        MainActivity.ACTION_SEND,
                        R.color.primary_navy_medium,
                        R.drawable.vector_qr_offset
                ))
        return pages
    }

    private fun checkLatestAnnouncement(txList: MutableList<Any>) {
        // If user hasn't completed onboarding, ignore announcements
        buyDataManager.canBuy
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({ buyAllowed ->
                    if (buyAllowed && view.shouldShowBuy && isOnboardingComplete()) {
                        if (!prefsUtil.getValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, false)
                                && txList.isNotEmpty()) {
                            prefsUtil.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN, true)
                            showAnnouncement()
                        } else {
                            dismissAnnouncement()
                        }
                    } else {
                        dismissAnnouncement()
                    }
                }, { Timber.e(it) })
    }

    private fun showAnnouncement() {
        // Don't add the announcement to an empty UI state, don't add it if there already is one
        if (displayList.isNotEmpty() && displayList.none { it is AnnouncementData }) {
            // In the future, the announcement data may be parsed from an endpoint. For now, here is fine
            val announcementData = AnnouncementData(
                    title = R.string.onboarding_available_now,
                    description = R.string.onboarding_buy_details,
                    link = R.string.onboarding_buy_bitcoin,
                    image = R.drawable.vector_wallet_offset,
                    emoji = "🎉",
                    closeFunction = { dismissAnnouncement() },
                    linkFunction = { view.startBuyActivity() }
            )
            displayList.add(0, announcementData)
            view.onTransactionsUpdated(displayList)
        }
    }

    private fun dismissAnnouncement() {
        prefsUtil.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        if (displayList.any { it is AnnouncementData }) {
            displayList.removeAll { it is AnnouncementData }
            view.onTransactionsUpdated(displayList)
        }
    }

    private fun getFormattedPriceString(): String {
        val lastPrice = getLastPrice(getFiatCurrency())
        val fiatSymbol = exchangeRateFactory.getSymbol(getFiatCurrency())
        val format = DecimalFormat().apply { minimumFractionDigits = 2 }

        return stringUtils.getFormattedString(
                R.string.current_price_btc,
                "$fiatSymbol${format.format(lastPrice)}"
        )
    }

    private fun getBalanceString(isBTC: Boolean, btcBalance: Long): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = exchangeRateFactory.getLastPrice(strFiat) * (btcBalance / 1e8)

        return if (isBTC) {
            "${monetaryUtil.getDisplayAmountWithFormatting(btcBalance)} ${getDisplayUnits()}"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
        }
    }

    private fun getLastPrice(fiat: String) = exchangeRateFactory.getLastPrice(fiat)

    private fun getDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

}