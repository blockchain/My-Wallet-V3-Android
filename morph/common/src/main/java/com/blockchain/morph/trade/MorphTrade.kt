package com.blockchain.morph.trade

interface MorphTrade {

    val timestamp: Long

    val status: MorphTrade.Status

    val hashOut: String?

    val quote: MorphTradeOrder

    enum class Status(private val text: String) {
        UNKNOWN("unknown"),
        NO_DEPOSITS("no_deposits"),
        RECEIVED("received"),
        COMPLETE("complete"),
        FAILED("failed"),
        RESOLVED("resolved"),
        REFUNDED("refunded"),
        REFUND_IN_PROGRESS("refund_in_progress"),
        EXPIRED("expired"),
        IN_PROGRESS("in_progress");

        override fun toString(): String {
            return text
        }
    }

    fun enoughInfoForDisplay(): Boolean
}
