package com.blockchain.morph.ui.homebrew.exchange

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.blockchain.morph.ui.R
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.support.v7.widget.Toolbar


class ExchangeLockedActivity : AppCompatActivity() {

    private lateinit var doneButton: Button
    private lateinit var toolbar: Toolbar

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

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.exchange_locked)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

}
