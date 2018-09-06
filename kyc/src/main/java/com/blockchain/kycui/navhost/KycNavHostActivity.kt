package com.blockchain.kycui.navhost

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.view.animation.DecelerateInterpolator
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.address.KycHomeAddressFragment
import com.blockchain.kycui.complete.ApplicationCompleteFragment
import com.blockchain.kycui.mobile.entry.KycMobileEntryFragment
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.onfidosplash.OnfidoSplashFragment
import com.blockchain.kycui.profile.KycProfileFragment
import com.blockchain.kycui.profile.models.ProfileModel
import com.blockchain.kycui.status.KycStatusActivity
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.invisibleIf
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.activity_kyc_nav_host.frame_layout_fragment_wrapper as fragmentWrapper
import kotlinx.android.synthetic.main.activity_kyc_nav_host.nav_host as navHostFragment
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_kyc as progressIndicator
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_loading_user as progressLoadingUser
import kotlinx.android.synthetic.main.activity_kyc_nav_host.toolbar_kyc as toolBar

class KycNavHostActivity : BaseMvpActivity<KycNavHostView, KycNavHostPresenter>(),
    KycProgressListener, KycNavHostView {

    private val presenter: KycNavHostPresenter by inject()
    private val navController by unsafeLazy { findNavController(navHostFragment) }
    private val currentFragment: Fragment?
        get() = navHostFragment.childFragmentManager.findFragmentById(R.id.nav_host)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        setupToolbar(toolBar, R.string.kyc_splash_title)

        onViewReady()
    }

    override fun setHostTitle(title: Int) {
        toolBar.title = getString(title)
    }

    override fun displayLoading(loading: Boolean) {
        fragmentWrapper.invisibleIf(loading)
        progressLoadingUser.invisibleIf(!loading)
    }

    override fun navigateToStatus() {
        KycStatusActivity.start(this)
    }

    override fun showErrorToastAndFinish(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
        finish()
    }

    override fun navigateToCountrySelection() {
        navController.navigate(R.id.kycCountrySelectionFragment)
    }

    override fun navigateToProfile(countryCode: String) {
        val args = KycProfileFragment.bundleArgs(countryCode)
        // Pop forward two pages
        navigateToCountrySelection()
        navController.navigate(R.id.kycProfileFragment, args)
    }

    override fun navigateToAddress(profileModel: ProfileModel, countryCode: String) {
        // Pop forward three pages
        navigateToProfile(countryCode)
        val args = KycHomeAddressFragment.bundleArgs(profileModel)
        navController.navigate(R.id.kycHomeAddressFragment, args)
    }

    override fun navigateToMobileEntry(profileModel: ProfileModel, countryCode: String) {
        navigateToAddress(profileModel, countryCode)
        val args = KycMobileEntryFragment.bundleArgs(countryCode)
        navController.navigate(R.id.kycPhoneNumberFragment, args)
    }

    override fun navigateToOnfido(profileModel: ProfileModel, countryCode: String) {
        // Here we skip adding mobile verification to the back stack
        navigateToMobileEntry(profileModel, countryCode)
        val args = OnfidoSplashFragment.bundleArgs(countryCode)
        navController.navigate(R.id.onfidoSplashFragment, args)
    }

    override fun incrementProgress(kycStep: KycStep) {
        val progress =
            100 * (
                KycStep.values()
                    .takeWhile { it != kycStep }
                    .sumBy { it.relativeValue } + kycStep.relativeValue
                ) / KycStep.values().sumBy { it.relativeValue }

        updateProgressBar(progress)
    }

    override fun decrementProgress(kycStep: KycStep) {
        val progress =
            100 * KycStep.values()
                .takeWhile { it != kycStep }
                .sumBy { it.relativeValue } / KycStep.values().sumBy { it.relativeValue }

        updateProgressBar(progress)
    }

    private fun updateProgressBar(progress: Int) {
        ObjectAnimator.ofInt(progressIndicator, "progress", progress).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments.forEach { fragment ->
            fragment.childFragmentManager.fragments.forEach {
                it.onActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        // If on final page, close host Activity on navigate up
        if (currentFragment is ApplicationCompleteFragment ||
            // If navigating up unsuccessful, close host Activity
            !navController.navigateUp()
        ) {
            finish()
        }
    }

    override fun createPresenter(): KycNavHostPresenter = presenter

    override fun getView(): KycNavHostView = this

    override fun startLogoutTimer() = Unit

    companion object {

        fun start(context: Context) {
            Intent(context, KycNavHostActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}

interface KycProgressListener {

    fun setHostTitle(@StringRes title: Int)

    fun incrementProgress(kycStep: KycStep)

    fun decrementProgress(kycStep: KycStep)
}