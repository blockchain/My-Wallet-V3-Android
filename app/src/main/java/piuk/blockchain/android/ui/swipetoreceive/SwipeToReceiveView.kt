package piuk.blockchain.android.ui.swipetoreceive

import android.graphics.Bitmap
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.ui.base.View

interface SwipeToReceiveView : View {

    fun displayQrCode(bitmap: Bitmap)

    fun displayReceiveAddress(address: String)

    fun displayReceiveAccount(accountName: String)

    fun setUiState(@UiState.UiStateDef uiState: Int)

    fun displayCoinType(requestString: String)
}