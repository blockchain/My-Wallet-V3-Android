package com.blockchain.testutils

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.junit.Test

class FluentTestRuleTests {

    @Test
    fun `blocks are not run early`() {
        var x = 0
        before { x = 1 }.after { x = 2 }
        x `should be` 0
    }

    @Test
    fun `"before" block is run`() {
        var x = 0
        val rule = before { x = 1 }.after { }
        rule.runRule()

        x `should be` 1
    }

    @Test
    fun `"after" block is run`() {
        var x = 0
        val rule = before {}.after { x = 2 }
        rule.runRule()

        x `should be` 2
    }

    @Test
    fun `"after" block is run later than "before"`() {
        var x = 0
        val rule = before { x = 10 }.after { x = 20 }
        rule.runRule()

        x `should be` 20
    }

    @Test
    fun `statement block is run after "before" block`() {
        var x = 0
        val rule = before { x = 10 }.after { }
        rule.runRule { x = 11 }

        x `should be` 11
    }

    @Test
    fun `after block is run in event of exception`() {
        var x = 0
        val rule = before { throw RuntimeException() }.after { x = 30 };

        { rule.runRule() } `should throw` RuntimeException::class

        x `should be` 30
    }

    @Test
    fun `after block is run in event of exception in statement`() {
        var x = 0
        val rule = before { }.after { x = 30 };

        { rule.runRule { throw RuntimeException() } } `should throw` RuntimeException::class

        x `should be` 30
    }
}
