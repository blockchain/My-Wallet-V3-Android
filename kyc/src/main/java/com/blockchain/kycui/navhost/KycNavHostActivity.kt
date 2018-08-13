package com.blockchain.kycui.navhost

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.view.animation.DecelerateInterpolator
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.navhost.models.KycStep
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.activity_kyc_nav_host.nav_host as navHostFragment
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_kyc as progressIndicator
import kotlinx.android.synthetic.main.activity_kyc_nav_host.toolbar_kyc as toolBar

class KycNavHostActivity : BaseAuthActivity(), KycProgressListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        setupToolbar(toolBar, R.string.kyc_splash_title)
    }

    override fun setHostTitle(title: Int) {
        toolBar.title = getString(title)
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

    override fun onSupportNavigateUp(): Boolean = findNavController(navHostFragment).navigateUp()

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