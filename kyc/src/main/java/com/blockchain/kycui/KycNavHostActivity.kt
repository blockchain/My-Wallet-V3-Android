package com.blockchain.kycui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import kotlinx.android.synthetic.main.activity_kyc_nav_host.*
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.kyc.R

class KycNavHostActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        setupToolbar(toolbar_kyc as Toolbar, "Exchange")
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    companion object {

        fun start(context: Context) {
            Intent(context, KycNavHostActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}