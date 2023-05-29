package com.blockchain.transactions.upsell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.upsell.viewmodel.UpsellAnotherAssetNavigationEvent
import com.blockchain.transactions.upsell.viewmodel.UpsellAnotherAssetViewState
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class UpsellAnotherAssetBottomSheet :
    MVIBottomSheet<UpsellAnotherAssetViewState>(),
    KoinScopeComponent,
    NavigationRouter<UpsellAnotherAssetNavigationEvent> {

    interface Host : MVIBottomSheet.Host {
        fun launchBuyForAsset(networkTicker: String)
        fun launchBuy()
        fun onCloseUpsellAnotherAsset()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a SimpleBuyUpsellAnotherAsset.Host"
        )
    }

    override val scope: Scope
        get() = payloadScope

    private val analytics: Analytics by inject()

    private val assetJustBoughtTicker: String by lazy {
        arguments?.getString(ASSET_TICKER) ?: throw IllegalStateException("No asset ticker passed")
    }

    private val title: String by lazy {
        arguments?.getString(ARG_TITLE) ?: throw IllegalStateException("No asset ticker passed")
    }

    private val description: String by lazy {
        arguments?.getString(ARG_DESCRIPTION) ?: throw IllegalStateException("No asset ticker passed")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Card(shape = AppTheme.shapes.large, elevation = 0.dp, backgroundColor = AppTheme.colors.light) {
                    Column {
                        SheetHeader(
                            shouldShowDivider = false,
                            onClosePress = {
                                host.onCloseUpsellAnotherAsset()
                                analytics.logEvent(UpSellAnotherAssetDismissed)
                                dismiss()
                            },
                            modifier = Modifier.background(color = AppTheme.colors.light)
                        )

                        UpsellAnotherAssetScreen(
                            title = title,
                            description = description,
                            assetJustTransactedTicker = assetJustBoughtTicker,
                            onBuyMostPopularAsset = {
                                host.launchBuyForAsset(networkTicker = it)
                                dismiss()
                            },
                            onClose = {
                                host.onCloseUpsellAnotherAsset()
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStateUpdated(state: UpsellAnotherAssetViewState) {}

    override fun route(navigationEvent: UpsellAnotherAssetNavigationEvent) {}

    companion object {

        private const val ASSET_TICKER = "ASSET_TICKER"
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_DESCRIPTION = "ARG_DESCRIPTION"

        fun newInstance(
            assetTransactedTicker: String,
            title: String,
            description: String,
        ): UpsellAnotherAssetBottomSheet =
            UpsellAnotherAssetBottomSheet()
                .apply {
                    arguments = Bundle().apply {
                        putString(ASSET_TICKER, assetTransactedTicker)
                        putString(ARG_TITLE, title)
                        putString(ARG_DESCRIPTION, description)
                    }
                }
    }
}
