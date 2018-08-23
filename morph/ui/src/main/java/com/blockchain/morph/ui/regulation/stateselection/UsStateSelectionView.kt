package com.blockchain.morph.ui.regulation.stateselection

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View

internal interface UsStateSelectionView : View {

    fun onError(@StringRes message: Int)

    fun finishActivityWithResult(resultCode: Int)
}
