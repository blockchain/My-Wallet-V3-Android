package com.blockchain.morph.ui.homebrew.exchange

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.Button
import com.blockchain.morph.ui.R

class ExchangeConfirmationActivity : AppCompatActivity() {

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

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.confirm_exchange)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
