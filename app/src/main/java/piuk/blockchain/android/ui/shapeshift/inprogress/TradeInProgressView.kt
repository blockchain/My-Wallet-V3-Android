package piuk.blockchain.android.ui.shapeshift.inprogress

import piuk.blockchain.android.ui.shapeshift.models.TradeProgressUiState
import piuk.blockchain.androidcoreui.ui.base.View

interface TradeInProgressView : View {

    val depositAddress: String

    fun updateUi(uiState: TradeProgressUiState)
}