package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.core.price.HistoricalTimeSpan
import com.github.mikephil.charting.data.Entry
import piuk.blockchain.android.ui.coinview.presentation.CoinviewIntents
import piuk.blockchain.android.ui.coinview.presentation.CoinviewPriceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewModel
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewState

@Composable
fun Coinview(
    viewModel: CoinviewViewModel,
    backOnClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: CoinviewViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        CoinviewScreen(
            backOnClick = backOnClick,
            networkTicker = state.assetName,

            price = state.assetPrice,
            onChartEntryHighlighted = { entry ->
                viewModel.onIntent(CoinviewIntents.UpdatePriceForChartSelection(entry))
            },
            resetPriceInformation = {
                viewModel.onIntent(CoinviewIntents.ResetPriceSelection)
            },
            onNewTimeSpanSelected = { timeSpan ->
                viewModel.onIntent(CoinviewIntents.NewTimeSpanSelected(timeSpan))
            }
        )
    }
}

@Composable
fun CoinviewScreen(
    backOnClick: () -> Unit,
    networkTicker: String,
    price: CoinviewPriceState,
    onChartEntryHighlighted: (Entry) -> Unit,
    resetPriceInformation: () -> Unit,
    onNewTimeSpanSelected: (HistoricalTimeSpan) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NavigationBar(
            title = networkTicker,
            onBackButtonClick = backOnClick
        )

        AssetPrice(
            data = price,
            onChartEntryHighlighted = onChartEntryHighlighted,
            resetPriceInformation = resetPriceInformation,
            onNewTimeSpanSelected = onNewTimeSpanSelected
        )
    }
}

@Preview(name = "CoinviewScreen", showBackground = true)
@Composable
fun PreviewCoinviewScreen() {
    CoinviewScreen(
        backOnClick = {},
        networkTicker = "ETH",
        price = CoinviewPriceState.Loading,
        onChartEntryHighlighted = {},
        resetPriceInformation = {},
        onNewTimeSpanSelected = {}
    )
}
