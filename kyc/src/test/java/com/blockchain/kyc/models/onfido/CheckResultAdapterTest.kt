package com.blockchain.kyc.models.onfido

import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.junit.Test

class CheckResultAdapterTest {

    @Test
    fun `from clear`() {
        CheckResultAdapter().fromJson("clear") `should equal` CheckResult.Clear
    }

    @Test
    fun `from consider`() {
        CheckResultAdapter().fromJson("consider") `should equal` CheckResult.Consider
    }

    @Test
    fun `from unidentified`() {
        CheckResultAdapter().fromJson("unidentified") `should equal` CheckResult.Unidentified
    }

    @Test
    fun `from unknown, should throw JsonDataException`() {
        {
            CheckResultAdapter().fromJson("")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to clear`() {
        CheckResultAdapter().toJson(CheckResult.Clear) `should equal` "clear"
    }

    @Test
    fun `to consider`() {
        CheckResultAdapter().toJson(CheckResult.Consider) `should equal` "consider"
    }

    @Test
    fun `to unidentified`() {
        CheckResultAdapter().toJson(CheckResult.Unidentified) `should equal` "unidentified"
    }
}