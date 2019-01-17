package com.blockchain.kyc.dev

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.kycui.navhost.models.CampaignType

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun launchKycForAirdrop(view: View) {
        KycNavHostActivity.start(this, CampaignType.Sunriver)
    }

    fun launchKycForSwap(view: View) {
        KycNavHostActivity.start(this, CampaignType.Swap)
    }
}
