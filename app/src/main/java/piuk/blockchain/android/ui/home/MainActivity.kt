package piuk.blockchain.android.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.FIND_VIEWS_WITH_TEXT
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.blockchain.annotations.ButWhy
import com.blockchain.koin.scopedInject
import com.blockchain.lockbox.ui.LockboxLandingActivity
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.NotificationAppOpened
import com.blockchain.notifications.analytics.RequestAnalyticsEvents
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import com.blockchain.notifications.analytics.TransactionsAnalyticsEvents
import com.blockchain.notifications.analytics.activityShown
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.account.AccountActivity
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.dashboard.DashboardFragment
import piuk.blockchain.android.ui.home.analytics.SideNavEvent
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.status.KycStatusActivity
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.onboarding.OnboardingActivity
import piuk.blockchain.android.ui.pairingcode.PairingCodeActivity
import piuk.blockchain.android.ui.receive.ReceiveFragment
import piuk.blockchain.android.ui.send.SendFragment
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.swap.homebrew.exchange.host.HomebrewNavHostActivity
import piuk.blockchain.android.ui.swapintro.SwapIntroFragment
import piuk.blockchain.android.ui.thepit.PitLaunchBottomDialog
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.ui.tour.IntroTourAnalyticsEvent
import piuk.blockchain.android.ui.tour.IntroTourHost
import piuk.blockchain.android.ui.tour.IntroTourStep
import piuk.blockchain.android.ui.tour.SwapTourFragment
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.CameraPermissionListener
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import java.util.ArrayList

class MainActivity : MvpActivity<MainView, MainPresenter>(),
    HomeNavigator,
    MainView,
    IntroTourHost,
    ConfirmPaymentDialog.OnConfirmDialogInteractionListener {

    override val presenter: MainPresenter by scopedInject()
    override val view: MainView = this

    var drawerOpen = false
        internal set

    private var handlingResult = false

    private val _refreshAnnouncements = PublishSubject.create<Unit>()
    val refreshAnnouncements: Observable<Unit>
        get() = _refreshAnnouncements

    private var activityResultAction: () -> Unit = {}

    private var backPressed: Long = 0

    // Fragment callbacks for currency header
    private val touchOutsideViews = HashMap<View, OnTouchOutsideViewListener>()

    private val tabSelectedListener =
        AHBottomNavigation.OnTabSelectedListener { position, wasSelected ->

            presenter.doTestnetCheck()

            if (!wasSelected) {
                when (position) {
                    ITEM_SEND -> if (currentFragment !is SendFragment) {
                        // This is a bit of a hack to allow the selection of the correct button
                        // On the bottom nav bar, but without starting the fragment again
                        startSendFragment(null)
                        ViewUtils.setElevation(appbar_layout, 0f)
                        analytics.logEvent(SendAnalytics.SendTabItem)
                    }
                    ITEM_HOME -> {
                        startDashboardFragment()
                        ViewUtils.setElevation(appbar_layout, 4f)
                    }
                    ITEM_ACTIVITY -> {
                        startActivitiesFragment()
                        ViewUtils.setElevation(appbar_layout, 0f)
                        analytics.logEvent(TransactionsAnalyticsEvents.TabItemClick)
                    }
                    ITEM_RECEIVE -> {
                        startReceiveFragment()
                        ViewUtils.setElevation(appbar_layout, 0f)
                        analytics.logEvent(RequestAnalyticsEvents.TabItemClicked)
                    }
                    ITEM_SWAP -> {
                        presenter.startSwapOrKyc(null, null)
                        analytics.logEvent(SwapAnalyticsEvents.SwapTabItemClick)
                    }
                }
            }
            true
        }

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.content_frame)!!

    internal val activity: Context
        get() = this

    private val selectedAccountFromFragments: Int
        get() = (currentFragment as? ReceiveFragment)?.getSelectedAccountPosition() ?: -1

    private val menu: Menu
        get() = navigation_view.menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.hasExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
        }

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // No-op
            }

            override fun onDrawerOpened(drawerView: View) {
                drawerOpen = true
                if (tour_guide.isActive) {
                    setTourMenuView()
                }
                analytics.logEvent(SideNavEvent.SideMenuOpenEvent)
            }

            override fun onDrawerClosed(drawerView: View) {
                drawerOpen = false
            }

            override fun onDrawerStateChanged(newState: Int) {
                // No-op
            }
        })

        // Set up toolbar_constraint
        toolbar_general.navigationIcon = ContextCompat.getDrawable(this, R.drawable.vector_menu)
        toolbar_general.title = ""
        setSupportActionBar(toolbar_general)

        // Styling
        bottom_navigation.apply {
            addItems(toolbarNavigationItems())
            accentColor = ContextCompat.getColor(context, R.color.bottom_toolbar_icon_active)
            inactiveColor = ContextCompat.getColor(context, R.color.bottom_toolbar_icon_inactive)

            titleState = AHBottomNavigation.TitleState.ALWAYS_SHOW
            isForceTint = true

            setUseElevation(true)
            setTitleTypeface(ResourcesCompat.getFont(context, R.font.inter_medium))

            setTitleTextSizeInSp(10.0f, 10.0f)

            // Select Dashboard by default
            setOnTabSelectedListener(tabSelectedListener)
            currentItem = ITEM_HOME
        }
    }

    override fun onResume() {
        super.onResume()
        activityResultAction().also {
            activityResultAction = {}
        }
        // This can null out in low memory situations, so reset here
        navigation_view.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
        presenter.updateTicker()

        if (!handlingResult) {
            resetUi()
        }

        handlingResult = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_qr_main -> {
                requestScan()
                analytics.logEvent(SendAnalytics.QRButtonClicked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingResult = true
        // We create a lambda so we handle the result after the view is attached to the presenter (onResume)
        activityResultAction = {
            if (resultCode == RESULT_OK && requestCode == SCAN_URI &&
                data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null
            ) {
                val strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT)
                handlePredefinedInput(strResult, false)
            } else if (requestCode == SETTINGS_EDIT || requestCode == ACCOUNT_EDIT || requestCode == KYC_STARTED) {
                replaceContentFragment(DashboardFragment.newInstance())
                // Reset state in case of changing currency etc
                bottom_navigation.currentItem = ITEM_HOME

                // Pass this result to balance fragment
                for (fragment in supportFragmentManager.fragments) {
                    fragment.onActivityResult(requestCode, resultCode, data)
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onBackPressed() {
        if (tour_guide.isActive) {
            tour_guide.stop()
        }
        val f = currentFragment
        val backHandled = when {
            drawerOpen -> {
                drawer_layout.closeDrawers()
                true
            }
            f is ActivitiesFragment -> f.onBackPressed()
            f is SendFragment -> f.onBackPressed()
            f is ReceiveFragment -> f.onBackPressed()
            f is DashboardFragment -> f.onBackPressed()
            f is SwapIntroFragment -> f.onBackPressed()
            else -> {
                // Switch to balance fragment - it's not clear, though,
                // how we can ever wind up here...
                startReceiveFragment()
                true
            }
        }

        if (!backHandled) {
            if (backPressed + BuildConfig.EXIT_APP_COOLDOWN_MILLIS > System.currentTimeMillis()) {
                presenter.clearLoginState()
            } else {
                showExitConfirmToast()
                backPressed = System.currentTimeMillis()
            }
        }
    }

    private fun setTourMenuView() {
        val item = menu.findItem(R.id.nav_simple_buy)

        val out = ArrayList<View>()
        drawer_layout.findViewsWithText(out, item.title, FIND_VIEWS_WITH_TEXT)

        if (out.isNotEmpty()) {
            val menuView = out[0]
            tour_guide.setDeferredTriggerView(menuView, offsetX = -menuView.width / 3)
        }
    }

    private fun showExitConfirmToast() {
        ToastCustom.makeText(this,
            getString(R.string.exit_confirm),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_GENERAL)
    }

    private fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this@MainActivity, CaptureActivity::class.java)
            startActivityForResult(intent, SCAN_URI)
        } else {
            ToastCustom.makeText(
                this@MainActivity,
                getString(R.string.camera_unavailable),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    private fun handlePredefinedInput(strResult: String, isDeeplinked: Boolean) {
        when {
            strResult.isBTCorBCHAddress() -> disambiguateBTCandBCHQrScans(strResult)
            strResult.isETHAddress() -> disambiguateETHQrScans(strResult)
            strResult.isHttpUri() -> presenter.handlePossibleDeepLink(strResult)
            else -> startSendFragment(strResult, isDeeplinked)
        }
    }

    private fun disambiguateBTCandBCHQrScans(uri: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_currency)
            .setMessage(R.string.confirm_currency_message)
            .setCancelable(true)
            .setPositiveButton(R.string.bitcoin_cash) { _, _ ->
                setCurrentCryptoCurrency(CryptoCurrency.BCH)
                startSendFragment(uri)
            }
            .setNegativeButton(R.string.bitcoin) { _, _ ->
                setCurrentCryptoCurrency(CryptoCurrency.BTC)
                startSendFragment(uri)
            }
            .create()
            .show()
    }

    private fun disambiguateETHQrScans(uri: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_currency)
            .setMessage(R.string.confirm_currency_message)
            .setCancelable(true)
            .setPositiveButton(R.string.ether) { _, _ ->
                setCurrentCryptoCurrency(CryptoCurrency.ETHER)
                startSendFragment(uri)
            }
            .setNegativeButton(R.string.usd_pax_1) { _, _ ->
                setCurrentCryptoCurrency(CryptoCurrency.PAX)
                startSendFragment(uri)
            }
            .create()
            .show()
    }

    private fun String.isHttpUri(): Boolean = startsWith("http")
    private fun String.isBTCorBCHAddress(): Boolean = FormatsUtil.isValidBitcoinAddress(this)
    private fun String.isETHAddress(): Boolean = FormatsUtil.isValidEthereumAddress(this)

    private fun selectDrawerItem(menuItem: MenuItem) {
        analytics.logEvent(SideNavEvent(menuItem.itemId))
        when (menuItem.itemId) {
            R.id.nav_lockbox -> LockboxLandingActivity.start(this)
            R.id.nav_backup -> launchBackupFunds()
            R.id.nav_debug_swap -> HomebrewNavHostActivity.start(this, presenter.defaultCurrency)
            R.id.nav_the_exchange -> presenter.onThePitMenuClicked()
            R.id.nav_simple_buy -> presenter.onSimpleBuyClicked()
            R.id.nav_airdrops -> AirdropCentreActivity.start(this)
            R.id.nav_addresses -> startActivityForResult(Intent(this, AccountActivity::class.java), ACCOUNT_EDIT)
            R.id.login_web_wallet -> PairingCodeActivity.start(this)
            R.id.nav_settings -> startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_EDIT)
            R.id.nav_support -> onSupportClicked()
            R.id.nav_logout -> showLogoutDialog()
        }
        drawer_layout.closeDrawers()
    }

    override fun launchSimpleBuy() {
        analytics.logEvent(SimpleBuyAnalytics.SIMPLE_BUY_SIDE_NAV)
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                launchFromDashboard = true
            )
        )
    }

    override fun showProgress() {
        progress.visible()
    }

    override fun hideProgress() {
        progress.gone()
    }

    override fun launchThePitLinking(linkId: String) {
        PitPermissionsActivity.start(this, linkId)
    }

    override fun launchThePit() {
        PitLaunchBottomDialog.launch(this)
    }

    override fun setPitEnabled(enabled: Boolean) {
        setPitVisible(enabled)
    }

    override fun setSimpleBuyEnabled(enabled: Boolean) {
        val menu = menu
        menu.findItem(R.id.nav_simple_buy).isVisible = enabled
    }

    override fun launchBackupFunds(fragment: Fragment?, requestCode: Int) {
        fragment?.let {
            BackupWalletActivity.startForResult(it, requestCode)
        } ?: BackupWalletActivity.start(this)
    }

    override fun launchSetup2Fa() {
        SettingsActivity.startFor2Fa(this)
    }

    override fun launchVerifyEmail() {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
        }
    }

    override fun launchSetupFingerprintLogin() {
        OnboardingActivity.launchForFingerprints(this)
    }

    override fun launchTransfer() {
        bottom_navigation.getViewAtPosition(ITEM_RECEIVE).performClick()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.logout_wallet)
            .setMessage(R.string.ask_you_sure_logout)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                analytics.logEvent(AnalyticsEvents.Logout)
                presenter.unPair()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onSupportClicked() {
        analytics.logEvent(AnalyticsEvents.Support)
        calloutToExternalSupportLinkDlg(this, URL_BLOCKCHAIN_SUPPORT_PORTAL)
    }

    private fun resetUi() {
        toolbar_general.title = ""

        // Set selected appropriately.
        with(bottom_navigation) {
            when (currentFragment) {
                is DashboardFragment -> currentItem = ITEM_HOME
                is ActivitiesFragment -> currentItem = ITEM_ACTIVITY
                is SendFragment -> currentItem = ITEM_SEND
                is ReceiveFragment -> currentItem = ITEM_RECEIVE
            }
        }
    }

    private fun requestScan() {

        val fragment = currentFragment::class.simpleName ?: "unknown"

        analytics.logEvent(object : AnalyticsEvent {
            override val event = "qr_scan_requested"
            override val params = mapOf("fragment" to fragment)
        })

        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(coordinator_layout, R.string.request_camera_permission)
            .withButton(android.R.string.ok) { requestScan() }
            .build()

        val grantedPermissionListener = CameraPermissionListener(analytics, {
            startScanActivity()
        })

        val compositePermissionListener =
            CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(compositePermissionListener)
            .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
            .check()
    }

    private fun startSingleActivity(clazz: Class<*>) {
        val intent = Intent(this@MainActivity, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun kickToLauncherPage() {
        startSingleActivity(LauncherActivity::class.java)
    }

    override fun showProgressDialog(message: Int) {
        super.showProgressDialog(message, null)
    }

    override fun hideProgressDialog() {
        super.dismissProgressDialog()
    }

    override fun launchKyc(campaignType: CampaignType) {
        KycNavHostActivity.startForResult(this, campaignType, KYC_STARTED)
    }

    override fun launchSwap(
        defCurrency: String,
        fromCryptoCurrency: CryptoCurrency?,
        toCryptoCurrency: CryptoCurrency?
    ) {
        HomebrewNavHostActivity.start(context = this,
            defaultCurrency = defCurrency,
            fromCryptoCurrency = fromCryptoCurrency,
            toCryptoCurrency = toCryptoCurrency)
    }

    override fun launchSwapOrKyc(targetCurrency: CryptoCurrency?, fromCryptoCurrency: CryptoCurrency?) {
        presenter.startSwapOrKyc(toCurrency = targetCurrency, fromCurrency = fromCryptoCurrency)
    }

    override fun onHandleInput(strUri: String) {
        handlePredefinedInput(strUri, true)
    }

    override fun getStartIntent(): Intent {
        return intent
    }

    private fun setPitVisible(visible: Boolean) {
        val menu = menu
        menu.findItem(R.id.nav_the_exchange).isVisible = visible
    }

    override fun clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java)!!.removeAllDynamicShortcuts()
        }
    }

    @ButWhy("Who calls this... it looks crazy")
    override fun onChangeFeeClicked() {
        val fragment = supportFragmentManager
            .findFragmentByTag(SendFragment::class.java.simpleName) as? SendFragment
        fragment?.onChangeFeeClicked()
    }

    @ButWhy("Who calls this")
    // Turns out this is called by the confirm transfer dlg. That can, however, be invoked from
    // both the account edit screen (when transferring between wallets) and the send screen.
    // (As can onChangeFeeClicked()) so the send fragment might not exist. This needs cleaning up. TODO
    override fun onSendClicked() {
        val fragment = supportFragmentManager
            .findFragmentByTag(SendFragment::class.java.simpleName) as? SendFragment
        fragment?.onSendClicked()
    }

    override fun showTestnetWarning() {
        val snack = Snackbar.make(
            coordinator_layout,
            R.string.testnet_warning,
            Snackbar.LENGTH_SHORT
        )
        val view = snack.view
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.product_red_medium))
        snack.show()
    }

    override fun enableSwapButton(isEnabled: Boolean) {
        if (isEnabled) {
            bottom_navigation.enableItemAtPosition(ITEM_SWAP)
        } else {
            bottom_navigation.disableItemAtPosition(ITEM_SWAP)
        }
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun displayDialog(title: Int, message: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun displayLockboxMenu(lockboxAvailable: Boolean) {
        menu.findItem(R.id.nav_lockbox).isVisible = lockboxAvailable
    }

    override fun showHomebrewDebugMenu() {
        menu.findItem(R.id.nav_debug_swap).isVisible = true
    }

    fun setOnTouchOutsideViewListener(
        view: View,
        onTouchOutsideViewListener: OnTouchOutsideViewListener
    ) {
        touchOutsideViews[view] = onTouchOutsideViewListener
    }

    override fun showNavigation() {
        bottom_navigation.restoreBottomNavigation()
        bottom_navigation.isBehaviorTranslationEnabled = true
    }

    override fun hideNavigation() {
        bottom_navigation.hideBottomNavigation()
        bottom_navigation.isBehaviorTranslationEnabled = false
    }

    override fun gotoSendFor(asset: CryptoCurrency) {
        setCurrentCryptoCurrency(asset)
        startSendFragment(null)
    }

    override fun gotoSendFor(account: SingleAccount) {
        if (account is CryptoAccount) {
            setCurrentCryptoCurrency(account.asset)
            startSendFragment(null)
        }
    }

    private fun startSendFragment(input: String?, isDeeplinked: Boolean = false) {
        setCurrentTabItem(ITEM_SEND)

        ViewUtils.setElevation(appbar_layout, 0f)

        val sendFragment = SendFragment.newInstance(input, isDeeplinked)
        replaceContentFragment(sendFragment)
    }

    override fun gotoReceiveFor(account: SingleAccount) {
        if (account is CryptoAccount) {
            setCurrentCryptoCurrency(account.asset)
            setCurrentTabItem(ITEM_RECEIVE)
            ViewUtils.setElevation(appbar_layout, 0f)
            startReceiveFragment()
        }
    }

    @Deprecated("Remove when new send/receive is fully implemented")
    private fun setCurrentCryptoCurrency(asset: CryptoCurrency) {
        // The old send framework does not support the newer assets, so filter them
        // out here, else the receive and send fragments can crash if the global "selected"
        // currency is in the not-supported set. See AND-3448
        val oldSendSupported = setOf(
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.BCH,
            CryptoCurrency.XLM,
            CryptoCurrency.PAX
        )
        if (oldSendSupported.contains(asset)) {
            presenter.cryptoCurrency = asset
        }
    }

    private fun startReceiveFragment() {
        setCurrentTabItem(ITEM_RECEIVE)

        ViewUtils.setElevation(appbar_layout, 0f)
        val receiveFragment = ReceiveFragment.newInstance(selectedAccountFromFragments)
        replaceContentFragment(receiveFragment)
    }

    override fun gotoDashboard() {
        bottom_navigation.currentItem = ITEM_HOME
    }

    private fun startDashboardFragment() {
        val fragment = DashboardFragment.newInstance()
        replaceContentFragment(fragment)
    }

    override fun gotoActivityFor(account: BlockchainAccount?) {
        // Once coincore is fully integrated, we won't care about setting the
        // asset dropdown to the currently/active cryptocurrency.
        // For now, we'll only set it if we're actually looking at a crypto asset
        if (account != null && account is CryptoAccount) {
            setCurrentCryptoCurrency(account.asset)
        }
        startActivitiesFragment(account)
    }

    override fun resumeSimpleBuyKyc() {
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                launchKycResume = true
            )
        )
    }

    override fun startSimpleBuy() {
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                launchFromDashboard = true
            )
        )
    }

    private fun startActivitiesFragment(account: BlockchainAccount? = null) {
        setCurrentTabItem(ITEM_ACTIVITY)
        val fragment = ActivitiesFragment.newInstance(account)
        replaceContentFragment(fragment)
        toolbar_general.title = ""
        analytics.logEvent(activityShown(account?.label ?: "All Wallets"))
    }

    override fun refreshAnnouncements() {
        _refreshAnnouncements.onNext(Unit)
    }

    override fun launchKycIntro() {
        val swapIntroFragment = SwapIntroFragment.newInstance()
        replaceContentFragment(swapIntroFragment)
    }

    override fun launchSwapIntro() {
        setCurrentTabItem(ITEM_SWAP)

        ViewUtils.setElevation(appbar_layout, 0f)

        val swapIntroFragment = SwapIntroFragment.newInstance()
        replaceContentFragment(swapIntroFragment)
    }

    override fun launchPendingVerificationScreen(campaignType: CampaignType) {
        KycStatusActivity.start(this, campaignType)
    }

    override fun shouldIgnoreDeepLinking() =
        (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0

    private fun replaceContentFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment, fragment.javaClass.simpleName)
            .commitAllowingStateLoss()

        showNavigation()
    }

    /*** Silently switch the current tab in the tab_bar */
    private fun setCurrentTabItem(item: Int) {
        bottom_navigation.apply {
            removeOnTabSelectedListener()
            currentItem = item
            setOnTabSelectedListener(tabSelectedListener)
        }
    }

    companion object {

        val TAG: String = MainActivity::class.java.simpleName

        const val SCAN_URI = 2007
        const val ACCOUNT_EDIT = 2008
        const val SETTINGS_EDIT = 2009
        const val KYC_STARTED = 2011

        // Keep these constants - the position of the toolbar items - and the generation of the toolbar items
        // together.
        private const val ITEM_ACTIVITY = 0
        private const val ITEM_SWAP = 1
        private const val ITEM_HOME = 2
        private const val ITEM_SEND = 3
        private const val ITEM_RECEIVE = 4

        private fun toolbarNavigationItems(): List<AHBottomNavigationItem> =
            listOf(AHBottomNavigationItem(
                R.string.toolbar_cmd_activity,
                R.drawable.ic_vector_toolbar_activity,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_swap,
                R.drawable.ic_vector_toolbar_swap,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_home,
                R.drawable.ic_vector_toolbar_home,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_send,
                R.drawable.ic_vector_toolbar_send,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_receive_crypto,
                R.drawable.ic_vector_toolbar_receive,
                R.color.white
            ))

        fun start(context: Context, bundle: Bundle) {
            Intent(context, MainActivity::class.java).apply {
                putExtras(bundle)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(this)
            }
        }
    }

    override fun launchIntroTour() {

        bottom_navigation.restoreBottomNavigation(false)

        val tourSteps = listOf(
            IntroTourStep(
                name = "Step_One",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_HOME) },
                analyticsEvent = IntroTourAnalyticsEvent.IntroPortfolioViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_home,
                msgTitle = R.string.tour_step_one_title,
                msgBody = R.string.tour_step_one_body_1,
                msgButton = R.string.tour_step_one_btn
            ),
            IntroTourStep(
                name = "Step_Two",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_SEND) },
                analyticsEvent = IntroTourAnalyticsEvent.IntroSendViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_send,
                msgTitle = R.string.tour_step_two_title,
                msgBody = R.string.tour_step_two_body,
                msgButton = R.string.tour_step_two_btn
            ),
            IntroTourStep(
                name = "Step_Three",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_RECEIVE) },
                analyticsEvent = IntroTourAnalyticsEvent.IntroRequestViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_receive,
                msgTitle = R.string.tour_step_three_title,
                msgBody = R.string.tour_step_three_body,
                msgButton = R.string.tour_step_three_btn
            ),
            IntroTourStep(
                name = "Step_Four",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_SWAP) },
                triggerClick = {
                    replaceContentFragment(SwapTourFragment.newInstance())
                },
                analyticsEvent = IntroTourAnalyticsEvent.IntroSwapViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_swap,
                msgTitle = R.string.tour_step_four_title,
                msgBody = R.string.tour_step_four_body,
                msgButton = R.string.tour_step_four_btn
            )
        )

        tour_guide.start(this, tourSteps)
    }

    override fun onTourFinished() {
        drawer_layout.closeDrawers()
        startDashboardFragment()
    }

    override fun showTourDialog(dlg: BottomSheetDialogFragment) {
        val fm = supportFragmentManager
        dlg.show(fm, "TOUR_SHEET")
    }
}
