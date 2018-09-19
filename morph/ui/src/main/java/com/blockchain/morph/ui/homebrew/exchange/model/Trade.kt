package com.blockchain.morph.ui.homebrew.exchange.model

import android.os.Parcelable
import com.blockchain.morph.trade.MorphTrade
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Trade(
    val id: String,
    val state: MorphTrade.Status,
    val currency: String,
    val price: String,
    val pair: String,
    val quantity: String,
    val createdAt: String,
    val depositQuantity: String
) : Parcelable