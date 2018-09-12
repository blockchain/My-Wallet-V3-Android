package com.blockchain.morph.ui.homebrew.exchange

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.blockchain.morph.ui.R
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ExchangeLockedActivity : BaseAuthActivity() {
    private lateinit var doneButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_locked)

        doneButton = findViewById(R.id.button_done)

        doneButton.setOnClickListener {
            val intent = Intent(this, ExchangeHistoryActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        setupToolbar(R.id.toolbar_constraint, R.string.exchange_locked)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}