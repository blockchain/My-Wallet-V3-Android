package com.blockchain.morph.ui.homebrew.exchange

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.Button
import com.blockchain.morph.ui.R
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ExchangeConfirmationActivity : BaseAuthActivity() {

    private lateinit var sendButton: Button
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_confirmation)

        sendButton = findViewById(R.id.button_send_now)

        sendButton.setOnClickListener {
            val intent = Intent(this, ExchangeLockedActivity::class.java)
            startActivity(intent)
            finish()
        }
        setupToolbar(R.id.toolbar_constraint, R.string.confirm_exchange)
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }
}
