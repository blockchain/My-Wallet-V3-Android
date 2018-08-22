package com.blockchain.morph.ui.detail

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

data class TradeDetailUiState(
    @StringRes val title: Int,
    @StringRes val heading: Int,
    val message: String,
    @DrawableRes val icon: Int,
    @ColorRes val receiveColor: Int
)