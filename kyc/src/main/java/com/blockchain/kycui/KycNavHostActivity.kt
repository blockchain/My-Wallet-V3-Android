package com.blockchain.kycui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IntRange
import android.support.v7.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment.findNavController
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.kyc.R
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_kyc_nav_host.nav_host as navHostFragment
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_kyc as progressIndicator
import kotlinx.android.synthetic.main.activity_kyc_nav_host.toolbar_kyc as toolBar
import android.R.attr.animation
import android.view.animation.DecelerateInterpolator



class KycNavHostActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        setupToolbar(toolBar as Toolbar, "Exchange")
    }

    fun setKycProgress(@IntRange(from = 0, to = 100) progress: Int) {
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