package com.blockchain.kyc.models.nabu

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class UserStateAdapterTest {

    @Test
    fun `from none`() {
        UserStateAdapter().fromJson("NONE") `should equal` UserState.None
    }

    @Test
    fun `from created`() {
        UserStateAdapter().fromJson("CREATED") `should equal` UserState.Created
    }

    @Test
    fun `from active`() {
        UserStateAdapter().fromJson("ACTIVE") `should equal` UserState.Active
    }

    @Test
    fun `from blocked`() {
        UserStateAdapter().fromJson("BLOCKED") `should equal` UserState.Blocked
    }

    @Test
    fun `from unknown should throw exception`() {
        {
            UserStateAdapter().fromJson("malformed")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to none`() {
        UserStateAdapter().toJson(UserState.None) `should equal` "NONE"
    }

    @Test
    fun `to created`() {
        UserStateAdapter().toJson(UserState.Created) `should equal` "CREATED"
    }

    @Test
    fun `to active`() {
        UserStateAdapter().toJson(UserState.Active) `should equal` "ACTIVE"
    }

    @Test
    fun `to blocked`() {
        UserStateAdapter().toJson(UserState.Blocked) `should equal` "BLOCKED"
    }
}