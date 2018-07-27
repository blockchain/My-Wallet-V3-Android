package com.blockchain.kyc.models

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class CheckResultAdapterTest {

    @Test
    fun `from clear`() {
        CheckResultAdapter().fromJson("clear")
            .run { this `should equal` CheckResult.Clear }
    }

    @Test
    fun `from consider`() {
        CheckResultAdapter().fromJson("consider")
            .run { this `should equal` CheckResult.Consider }
    }

    @Test
    fun `from unidentified`() {
        CheckResultAdapter().fromJson("unidentified")
            .run { this `should equal` CheckResult.Unidentified }
    }

    @Test
    fun `from unknown, should throw JsonDataException`() {
        {
            CheckResultAdapter().fromJson("")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to clear`() {
        CheckResultAdapter().toJson(CheckResult.Clear)
            .run { this `should equal` "clear" }
    }

    @Test
    fun `to consider`() {
        CheckResultAdapter().toJson(CheckResult.Consider)
            .run { this `should equal` "consider" }
    }

    @Test
    fun `to unidentified`() {
        CheckResultAdapter().toJson(CheckResult.Unidentified)
            .run { this `should equal` "unidentified" }
    }
}