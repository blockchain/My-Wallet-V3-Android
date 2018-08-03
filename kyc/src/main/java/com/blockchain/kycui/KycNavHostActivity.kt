package com.blockchain.kycui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IntRange
import android.support.v7.widget.Toolbar
import android.view.animation.DecelerateInterpolator
import androidx.navigation.fragment.NavHostFragment.findNavController
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.activity_kyc_nav_host.nav_host as navHostFragment
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_kyc as progressIndicator
import kotlinx.android.synthetic.main.activity_kyc_nav_host.toolbar_kyc as toolBar

class KycNavHostActivity : BaseAuthActivity(), KycProgressListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        setupToolbar(toolBar as Toolbar, "Exchange")
    }

    override fun onProgressUpdated(progress: Int) {
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
    fun onProgressUpdated(@IntRange(from = 0, to = 100) progress: Int)
}