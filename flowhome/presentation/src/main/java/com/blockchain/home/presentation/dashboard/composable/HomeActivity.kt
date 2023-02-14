package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.R
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponentItem
import com.blockchain.home.presentation.activity.common.ClickAction
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.get

fun LazyListScope.homeActivityScreen(
    activityState: ActivityViewState,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    wMode: WalletMode
) {
    (activityState.activity as? DataResource.Data)?.data?.get(TransactionGroup.Combined)?.takeIf { activity ->
        activity.isNotEmpty()
    }?.let { activities ->
        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            val analytics: Analytics = get()
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
            TableRowHeader(
                title = stringResource(R.string.ma_home_activity_title),
                actionTitle = stringResource(R.string.see_all),
                actionOnClick = {
                    openActivity()
                    analytics.logEvent(DashboardAnalyticsEvents.ActivitySeeAllClicked)
                }
            )
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        }
        paddedRoundedCornersItems(
            items = activities,
            key = { it.id },
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            ActivityComponentItem(
                component = it,
                onClick = { clickAction ->
                    (clickAction as? ClickAction.Stack)?.data?.let { data ->
                        openActivityDetail(data, wMode)
                    }
                }
            )
        }
    }
}
