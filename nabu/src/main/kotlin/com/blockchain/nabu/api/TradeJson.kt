package com.blockchain.nabu.api

import com.blockchain.nabu.api.TransactionStateAdapter.Companion.EXPIRED
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.FINISHED
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.FINISHED_DEPOSIT
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.PENDING_DEPOSIT
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.PENDING_EXECUTION
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.PENDING_REFUND
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.PENDING_WITHDRAWAL
import com.blockchain.nabu.api.TransactionStateAdapter.Companion.REFUNDED
import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import java.math.BigDecimal
import javax.management.remote.JMXConnectionNotification.FAILED

internal class TradeJson(
    val id: String,
    val state: TransactionState,
    val createdAt: String,
    val updatedAt: String,
    val pair: String,
    val refundAddress: String,
    val rate: BigDecimal?,
    val depositAddress: String,
    val deposit: Value,
    val withdrawalAddress: String,
    val withdrawal: Value,
    val withdrawalFee: Value,
    val fiatValue: Value,
    val depositTxHash: String?,
    val withdrawalTxHash: String?
) : JsonSerializable

sealed class TransactionState(val state: String) {

    object PendingExecution : TransactionState(PENDING_EXECUTION)
    object PendingDeposit : TransactionState(PENDING_DEPOSIT)
    object FinishedDeposit : TransactionState(FINISHED_DEPOSIT)
    object PendingWithdrawal : TransactionState(PENDING_WITHDRAWAL)
    object Finished : TransactionState(FINISHED)
    object PendingRefund : TransactionState(PENDING_REFUND)
    object Failed : TransactionState(FAILED)
    object Expired : TransactionState(EXPIRED)
    object Refunded : TransactionState(REFUNDED)
}

internal class TransactionStateAdapter {

    @FromJson
    fun fromJson(input: String): TransactionState = when (input) {
        PENDING_EXECUTION -> TransactionState.PendingExecution
        PENDING_DEPOSIT -> TransactionState.PendingDeposit
        FINISHED_DEPOSIT -> TransactionState.FinishedDeposit
        PENDING_WITHDRAWAL -> TransactionState.PendingWithdrawal
        FINISHED -> TransactionState.Finished
        PENDING_REFUND -> TransactionState.PendingRefund
        FAILED -> TransactionState.Failed
        EXPIRED -> TransactionState.Expired
        REFUNDED -> TransactionState.Refunded
        else -> throw JsonDataException("Unknown TransactionState: $input, unsupported data type")
    }

    @ToJson
    fun toJson(state: TransactionState): String = when (state) {
        TransactionState.PendingExecution -> PENDING_EXECUTION
        TransactionState.PendingDeposit -> PENDING_DEPOSIT
        TransactionState.FinishedDeposit -> FINISHED_DEPOSIT
        TransactionState.PendingWithdrawal -> PENDING_WITHDRAWAL
        TransactionState.Finished -> FINISHED
        TransactionState.PendingRefund -> PENDING_REFUND
        TransactionState.Failed -> FAILED
        TransactionState.Expired -> EXPIRED
        TransactionState.Refunded -> REFUNDED
    }

    internal companion object {

        const val PENDING_EXECUTION = "PENDING_EXECUTION"
        const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
        const val FINISHED_DEPOSIT = "FINISHED_DEPOSIT"
        const val PENDING_WITHDRAWAL = "PENDING_WITHDRAWAL"
        const val FINISHED = "FINISHED"
        const val PENDING_REFUND = "PENDING_REFUND"
        const val FAILED = "FAILED"
        const val EXPIRED = "EXPIRED"
        const val REFUNDED = "REFUNDED"
    }
}