package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.earn.EarnAssets
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeScreen(
    listState: LazyListState,
    assetActionsNavigation: AssetActionsNavigation,
    settingsNavigation: SettingsNavigation,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit,
    openReferral: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openMoreQuickActions: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0XFFF1F2F7),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ),
    ) {
        item {
            Balance(openSettings = {
                settingsNavigation.settings()
            })
        }

        item {
            QuickActions(
                assetActionsNavigation = assetActionsNavigation,
                openMoreQuickActions = openMoreQuickActions
            )
        }
        item {
            EmptyCard(
                onReceive = { assetActionsNavigation.navigate(AssetAction.Receive) },
                assetActionsNavigation = assetActionsNavigation,
                homeAssetsViewModel = getViewModel(scope = payloadScope),
                pkwActivityViewModel = getViewModel(scope = payloadScope),
                custodialActivityViewModel = getViewModel(scope = payloadScope)
            )
        }
        item {
            HomeAssets(
                assetActionsNavigation = assetActionsNavigation,
                openAllAssets = openCryptoAssets,
                openFiatActionDetail = openFiatActionDetail
            )
        }

        item {
            EarnAssets(assetActionsNavigation = assetActionsNavigation)
        }

        item {
            HomeActivity(
                openAllActivity = openActivity
            )
        }

        item {
            Referral(
                openReferral = openReferral
            )
        }

        item {
            HelpAndSupport()
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}
