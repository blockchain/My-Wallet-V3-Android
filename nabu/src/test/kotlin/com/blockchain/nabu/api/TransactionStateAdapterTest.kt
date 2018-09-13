package com.blockchain.nabu.api

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.shouldThrow
import org.junit.Test

class TransactionStateAdapterTest {

    @Test
    fun `from pending execution`() {
        TransactionStateAdapter()
            .fromJson("PENDING_EXECUTION") `should equal` TransactionState.PendingExecution
    }

    @Test
    fun `from pending_deposit`() {
        TransactionStateAdapter()
            .fromJson("PENDING_DEPOSIT") `should equal` TransactionState.PendingDeposit
    }

    @Test
    fun `from finished_deposit`() {
        TransactionStateAdapter()
            .fromJson("FINISHED_DEPOSIT") `should equal` TransactionState.FinishedDeposit
    }

    @Test
    fun `from pending_withdrawal`() {
        TransactionStateAdapter()
            .fromJson("PENDING_WITHDRAWAL") `should equal` TransactionState.PendingWithdrawal
    }

    @Test
    fun `from finished`() {
        TransactionStateAdapter()
            .fromJson("FINISHED") `should equal` TransactionState.Finished
    }

    @Test
    fun `from pending_refund`() {
        TransactionStateAdapter()
            .fromJson("PENDING_REFUND") `should equal` TransactionState.PendingRefund
    }

    @Test
    fun `from failed`() {
        TransactionStateAdapter()
            .fromJson("FAILED") `should equal` TransactionState.Failed
    }

    @Test
    fun `from expired`() {
        TransactionStateAdapter()
            .fromJson("EXPIRED") `should equal` TransactionState.Expired
    }

    @Test
    fun `from refunded`() {
        TransactionStateAdapter()
            .fromJson("REFUNDED") `should equal` TransactionState.Refunded
    }

    @Test
    fun `from invalid, should throw exception`() {
        {
            TransactionStateAdapter().fromJson("INVALID")
        } shouldThrow JsonDataException::class
    }

    @Test
    fun `to pending execution`() {
        TransactionStateAdapter()
            .toJson(TransactionState.PendingExecution) `should equal` "PENDING_EXECUTION"
    }

    @Test
    fun `to pending_deposit`() {
        TransactionStateAdapter()
            .toJson(TransactionState.PendingDeposit) `should equal` "PENDING_DEPOSIT"
    }

    @Test
    fun `to finished_deposit`() {
        TransactionStateAdapter()
            .toJson(TransactionState.FinishedDeposit) `should equal` "FINISHED_DEPOSIT"
    }

    @Test
    fun `to pending_withdrawal`() {
        TransactionStateAdapter()
            .toJson(TransactionState.PendingWithdrawal) `should equal` "PENDING_WITHDRAWAL"
    }

    @Test
    fun `to finished`() {
        TransactionStateAdapter()
            .toJson(TransactionState.Finished) `should equal` "FINISHED"
    }

    @Test
    fun `to pending_refund`() {
        TransactionStateAdapter()
            .toJson(TransactionState.PendingRefund) `should equal` "PENDING_REFUND"
    }

    @Test
    fun `to failed`() {
        TransactionStateAdapter()
            .toJson(TransactionState.Failed) `should equal` "FAILED"
    }

    @Test
    fun `to expired`() {
        TransactionStateAdapter()
            .toJson(TransactionState.Expired) `should equal` "EXPIRED"
    }

    @Test
    fun `to refunded`() {
        TransactionStateAdapter()
            .toJson(TransactionState.Refunded) `should equal` "REFUNDED"
    }
}