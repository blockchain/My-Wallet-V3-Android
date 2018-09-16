package com.blockchain.morph.ui.homebrew.exchange

import android.content.Intent
import android.os.Bundle
import com.blockchain.morph.ui.R
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryActivity
import kotlinx.android.synthetic.main.activity_homebrew_trade_locked.*
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ExchangeLockedActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_locked)

        button_done.setOnClickListener {
            val intent = Intent(this, TradeHistoryActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        setupToolbar(R.id.toolbar_constraint, R.string.exchange_locked)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}