package com.blockchain.morph.ui.regulation.stateselection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.blockchain.morph.regulation.americanStatesNamesList
import com.blockchain.morph.ui.R
import kotlinx.android.synthetic.main.activity_us_state_selection.*
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible

internal class UsStateSelectionActivity :
    BaseMvpActivity<UsStateSelectionView, UsStateSelectionPresenter>(),
    UsStateSelectionView {

    private val shapeShiftStateSelectionPresenter: UsStateSelectionPresenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_us_state_selection)
        setupToolbar(R.id.toolbar_general, R.string.morph_exchange)

        stateSelectError.invisible()

        btnConfirm.setOnClickListener { finishActivityWithResult(Activity.RESULT_CANCELED) }

        val states = americanStatesNamesList.toMutableList()
        states.add(getString(R.string.morph_select_state))

        val adapter = object :
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, states) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val v = super.getView(position, convertView, parent)

                if (position == count) {
                    with(v.findViewById(android.R.id.text1) as TextView) {
                        text = ""
                        hint = getItem(count)
                    }
                }

                return v
            }

            override fun getCount(): Int {
                return super.getCount() - 1 // Don't display last item. It is used as hint.
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerState.adapter = adapter
        spinnerState.setSelection(0)

        spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View,
                position: Int,
                id: Long
            ) {

                if (position == adapter.count) return

                stateSelectError.invisible()
                btnConfirm.gone()

                presenter.updateAmericanState(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        }

        onViewReady()
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    override fun onError(message: Int) {
        stateSelectError.visible()
        btnConfirm.visible()
        stateSelectError.setText(message)
    }

    override fun finishActivityWithResult(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    override fun createPresenter() = shapeShiftStateSelectionPresenter

    override fun getView() = this

    companion object {

        @JvmStatic
        fun startForResult(context: Activity, requestCode: Int) {
            context.startActivityForResult(
                Intent(
                    context,
                    UsStateSelectionActivity::class.java
                ), requestCode
            )
        }
    }
}
