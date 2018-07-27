package com.blockchain.kyc.models.onfido

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class CheckStatusAdapterTest {

    @Test
    fun `from awaiting applicant`() {
        CheckStatusAdapter().fromJson("awaiting_applicant") `should equal` CheckStatus.AwaitingApplicant
    }

    @Test
    fun `from awaiting data`() {
        CheckStatusAdapter().fromJson("awaiting_data") `should equal` CheckStatus.AwaitingData
    }

    @Test
    fun `from awaiting approval`() {
        CheckStatusAdapter().fromJson("awaiting_approval") `should equal` CheckStatus.AwaitingApproval
    }

    @Test
    fun `from complete`() {
        CheckStatusAdapter().fromJson("complete") `should equal` CheckStatus.Complete
    }

    @Test
    fun `from withdrawn`() {
        CheckStatusAdapter().fromJson("withdrawn") `should equal` CheckStatus.Withdrawn
    }

    @Test
    fun `from paused`() {
        CheckStatusAdapter().fromJson("paused") `should equal` CheckStatus.Paused
    }

    @Test
    fun `from cancelled`() {
        CheckStatusAdapter().fromJson("cancelled") `should equal` CheckStatus.Cancelled
    }

    @Test
    fun `from reopened`() {
        CheckStatusAdapter().fromJson("reopened") `should equal` CheckStatus.Reopened
    }

    @Test
    fun `from unknown, should throw JsonDataException`() {
        {
            CheckStatusAdapter().fromJson("")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to awaiting applicant`() {
        CheckStatusAdapter().toJson(CheckStatus.AwaitingApplicant) `should equal` "awaiting_applicant"
    }

    @Test
    fun `to awaiting data`() {
        CheckStatusAdapter().toJson(CheckStatus.AwaitingData) `should equal` "awaiting_data"
    }

    @Test
    fun `to awaiting approval`() {
        CheckStatusAdapter().toJson(CheckStatus.AwaitingApproval) `should equal` "awaiting_approval"
    }

    @Test
    fun `to complete`() {
        CheckStatusAdapter().toJson(CheckStatus.Complete) `should equal` "complete"
    }

    @Test
    fun `to withdrawn`() {
        CheckStatusAdapter().toJson(CheckStatus.Withdrawn) `should equal` "withdrawn"
    }

    @Test
    fun `to paused`() {
        CheckStatusAdapter().toJson(CheckStatus.Paused) `should equal` "paused"
    }

    @Test
    fun `to cancelled`() {
        CheckStatusAdapter().toJson(CheckStatus.Cancelled) `should equal` "cancelled"
    }

    @Test
    fun `to reopened`() {
        CheckStatusAdapter().toJson(CheckStatus.Reopened) `should equal` "reopened"
    }
}