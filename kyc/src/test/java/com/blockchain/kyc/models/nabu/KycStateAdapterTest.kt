package com.blockchain.kyc.models.nabu

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class KycStateAdapterTest {

    @Test
    fun `from none`() {
        KycStateAdapter().fromJson("NONE") `should equal` KycState.None
    }

    @Test
    fun `from pending`() {
        KycStateAdapter().fromJson("PENDING") `should equal` KycState.Pending
    }

    @Test
    fun `from rejected`() {
        KycStateAdapter().fromJson("REJECTED") `should equal` KycState.Rejected
    }

    @Test
    fun `from expired`() {
        KycStateAdapter().fromJson("EXPIRED") `should equal` KycState.Expired
    }

    @Test
    fun `from verified`() {
        KycStateAdapter().fromJson("VERIFIED") `should equal` KycState.Verified
    }

    @Test
    fun `from unknown should throw exception`() {
        {
            KycStateAdapter().fromJson("malformed")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to none`() {
        KycStateAdapter().toJson(KycState.None) `should equal` "NONE"
    }

    @Test
    fun `to pending`() {
        KycStateAdapter().toJson(KycState.Pending) `should equal` "PENDING"
    }

    @Test
    fun `to rejected`() {
        KycStateAdapter().toJson(KycState.Rejected) `should equal` "REJECTED"
    }

    @Test
    fun `to expired`() {
        KycStateAdapter().toJson(KycState.Expired) `should equal` "EXPIRED"
    }

    @Test
    fun `to verified`() {
        KycStateAdapter().toJson(KycState.Verified) `should equal` "VERIFIED"
    }
}