package piuk.blockchain.android.ui.shapeshift.stateselection

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View

interface ShapeShiftStateSelectionView : View {

    fun onError(@StringRes message: Int)

    fun finishActivityWithResult(resultCode: Int)
}