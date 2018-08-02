package com.blockchain.testutils

import org.amshove.kluent.`should equal to`
import org.junit.Test

class GetStringFromResourceTest {

    @Test
    fun `getStringFromResource should load test string`() {
        getStringFromResource("GetStringFromResourceTestFile.txt") `should equal to` "Test string"
    }

    @Test
    fun `asResource should load test string`() {
        "GetStringFromResourceTestFile.txt".asResource { it `should equal to` "Test string" }
    }
}