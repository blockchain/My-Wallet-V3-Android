package com.blockchain.morph.ui.regulation.stateselection

import android.app.Activity

interface UsStateSelectionActivityStarter {

    /**
     * Starts activity for return code. Returns the code.
     */
    fun startForResult(parent: Activity): Int
}
