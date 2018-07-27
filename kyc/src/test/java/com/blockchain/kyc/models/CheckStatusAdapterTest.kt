package com.blockchain.kyc.models

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class CheckStatusAdapterTest {

    @Test
    fun `from awaiting applicant`() {
        CheckStatusAdapter().fromJson("awaiting_applicant")
            .run { this `should equal` CheckStatus.AwaitingApplicant }
    }

    @Test
    fun `from awaiting data`() {
        CheckStatusAdapter().fromJson("awaiting_data")
            .run { this `should equal` CheckStatus.AwaitingData }
    }

    @Test
    fun `from awaiting approval`() {
        CheckStatusAdapter().fromJson("awaiting_approval")
            .run { this `should equal` CheckStatus.AwaitingApproval }
    }

    @Test
    fun `from complete`() {
        CheckStatusAdapter().fromJson("complete")
            .run { this `should equal` CheckStatus.Complete }
    }

    @Test
    fun `from withdrawn`() {
        CheckStatusAdapter().fromJson("withdrawn")
            .run { this `should equal` CheckStatus.Withdrawn }
    }

    @Test
    fun `from paused`() {
        CheckStatusAdapter().fromJson("paused")
            .run { this `should equal` CheckStatus.Paused }
    }

    @Test
    fun `from cancelled`() {
        CheckStatusAdapter().fromJson("cancelled")
            .run { this `should equal` CheckStatus.Cancelled }
    }

    @Test
    fun `from reopened`() {
        CheckStatusAdapter().fromJson("reopened")
            .run { this `should equal` CheckStatus.Reopened }
    }

    @Test
    fun `from unknown, should throw JsonDataException`() {
        {
            CheckStatusAdapter().fromJson("")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to awaiting applicant`() {
        CheckStatusAdapter().toJson(CheckStatus.AwaitingApplicant)
            .run { this `should equal` "awaiting_applicant" }
    }

    @Test
    fun `to awaiting data`() {
        CheckStatusAdapter().toJson(CheckStatus.AwaitingData)
            .run { this `should equal` "awaiting_data" }
    }

    @Test
    fun `to awaiting approval`() {
        CheckStatusAdapter().toJson(CheckStatus.AwaitingApproval)
            .run { this `should equal` "awaiting_approval" }
    }

    @Test
    fun `to complete`() {
        CheckStatusAdapter().toJson(CheckStatus.Complete)
            .run { this `should equal` "complete" }
    }

    @Test
    fun `to withdrawn`() {
        CheckStatusAdapter().toJson(CheckStatus.Withdrawn)
            .run { this `should equal` "withdrawn" }
    }

    @Test
    fun `to paused`() {
        CheckStatusAdapter().toJson(CheckStatus.Paused)
            .run { this `should equal` "paused" }
    }

    @Test
    fun `to cancelled`() {
        CheckStatusAdapter().toJson(CheckStatus.Cancelled)
            .run { this `should equal` "cancelled" }
    }

    @Test
    fun `to reopened`() {
        CheckStatusAdapter().toJson(CheckStatus.Reopened)
            .run { this `should equal` "reopened" }
    }
}