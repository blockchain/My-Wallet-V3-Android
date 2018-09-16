package com.blockchain.morph.ui.homebrew.exchange

import android.content.Intent
import android.os.Bundle
import com.blockchain.morph.ui.R
import kotlinx.android.synthetic.main.activity_homebrew_trade_confirmation.*
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ExchangeConfirmationActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_confirmation)

        button_send_now.setOnClickListener {
            val intent = Intent(this, ExchangeLockedActivity::class.java)
            startActivity(intent)
            finish()
        }
        setupToolbar(R.id.toolbar_constraint, R.string.confirm_exchange)
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }
}
