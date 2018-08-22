package com.blockchain.morph.trade

interface MorphTrade {

    val status: MorphTrade.Status

    val hashOut: String?

    val quote: MorphTradeOrder

    enum class Status(private val text: String) {
        UNKNOWN("unknown"),
        NO_DEPOSITS("no_deposits"),
        RECEIVED("received"),
        COMPLETE("complete"),
        FAILED("failed"),
        RESOLVED("resolved");

        override fun toString(): String {
            return text
        }
    }

    fun enoughInfoForDisplay(): Boolean
}
