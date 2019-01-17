package com.blockchain.kyc.models.nabu

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class KycTierStateAdapterTest {

    @Test
    fun `from none`() {
        KycTierStateAdapter().fromJson("NONE") `should equal` KycTierState.None
    }

    @Test
    fun `from none mixed case`() {
        KycTierStateAdapter().fromJson("None") `should equal` KycTierState.None
    }

    @Test
    fun `from rejected`() {
        KycTierStateAdapter().fromJson("REJECTED") `should equal` KycTierState.Rejected
    }

    @Test
    fun `from rejected mixed case`() {
        KycTierStateAdapter().fromJson("RejectED") `should equal` KycTierState.Rejected
    }

    @Test
    fun `from pending`() {
        KycTierStateAdapter().fromJson("PENDING") `should equal` KycTierState.Pending
    }

    @Test
    fun `from pending lower case`() {
        KycTierStateAdapter().fromJson("pending") `should equal` KycTierState.Pending
    }

    @Test
    fun `from verified`() {
        KycTierStateAdapter().fromJson("VERIFIED") `should equal` KycTierState.Verified
    }

    @Test
    fun `from verified lower case`() {
        KycTierStateAdapter().fromJson("verified") `should equal` KycTierState.Verified
    }

    @Test
    fun `from unknown should throw exception`() {
        {
            KycTierStateAdapter().fromJson("malformed")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to none`() {
        KycTierStateAdapter().toJson(KycTierState.None) `should equal` "NONE"
    }

    @Test
    fun `to rejected`() {
        KycTierStateAdapter().toJson(KycTierState.Rejected) `should equal` "REJECTED"
    }

    @Test
    fun `to pending`() {
        KycTierStateAdapter().toJson(KycTierState.Pending) `should equal` "PENDING"
    }

    @Test
    fun `to verified`() {
        KycTierStateAdapter().toJson(KycTierState.Verified) `should equal` "VERIFIED"
    }
}
