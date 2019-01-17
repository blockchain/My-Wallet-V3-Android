package com.blockchain.kycui.navhost

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.view.animation.DecelerateInterpolator
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.complete.ApplicationCompleteFragment
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.nabu.StartKyc
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.invisibleIf
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.KycNavXmlDirections
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.activity_kyc_nav_host.frame_layout_fragment_wrapper as fragmentWrapper
import kotlinx.android.synthetic.main.activity_kyc_nav_host.nav_host as navHostFragment
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_kyc as progressIndicator
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_loading_user as progressLoadingUser
import kotlinx.android.synthetic.main.activity_kyc_nav_host.toolbar_kyc as toolBar

internal class KycStarter : StartKyc {

    override fun startKycActivity(context: Any) {
        KycNavHostActivity.start(context as Context, CampaignType.Swap)
    }
}

class KycNavHostActivity : BaseMvpActivity<KycNavHostView, KycNavHostPresenter>(),
    KycProgressListener, KycNavHostView {

    private val presenter: KycNavHostPresenter by inject()

    private val navController by unsafeLazy { findNavController(navHostFragment) }
    private val currentFragment: Fragment?
        get() = navHostFragment.childFragmentManager.findFragmentById(R.id.nav_host)
    override val campaignType by unsafeLazy { intent.getSerializableExtra(EXTRA_CAMPAIGN_TYPE) as CampaignType }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        val title = when (campaignType) {
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.Sunriver -> R.string.sunriver_splash_title
        }
        setupToolbar(toolBar, title)

        onViewReady()
    }

    override fun setHostTitle(title: Int) {
        toolBar.title = getString(title)
    }

    override fun displayLoading(loading: Boolean) {
        fragmentWrapper.invisibleIf(loading)
        progressLoadingUser.invisibleIf(!loading)
    }

    override fun showErrorToastAndFinish(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
        finish()
    }

    override fun navigate(directions: NavDirections) {
        navController.navigate(directions)
    }

    override fun navigateToAirdropSplash() {
        navController.navigate(KycNavXmlDirections.ActionDisplayAirDropSplash())
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

    override fun hideBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
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

        private const val EXTRA_CAMPAIGN_TYPE = "piuk.blockchain.android.EXTRA_CAMPAIGN_TYPE"

        @JvmStatic
        fun start(context: Context, campaignType: CampaignType) {
            intentArgs(context, campaignType)
                .run { context.startActivity(this) }
        }

        @JvmStatic
        fun intentArgs(context: Context, campaignType: CampaignType): Intent =
            Intent(context, KycNavHostActivity::class.java)
                .apply { putExtra(EXTRA_CAMPAIGN_TYPE, campaignType) }
    }
}

interface KycProgressListener {

    val campaignType: CampaignType

    fun setHostTitle(@StringRes title: Int)

    fun incrementProgress(kycStep: KycStep)

    fun decrementProgress(kycStep: KycStep)

    fun hideBackButton()
}