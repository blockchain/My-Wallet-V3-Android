package com.blockchain.testutils

import org.junit.Test

class AssignableAssertionsTest {

    private interface I
    private class A : I
    private class B

    @Test
    fun self() {
        I::class `should be assignable from` I::class
    }

    @Test
    fun assignable() {
        I::class `should be assignable from` A::class
    }

    @Test(expected = AssertionError::class)
    fun `not assignable in this direction`() {
        A::class `should be assignable from` I::class
    }

    @Test(expected = AssertionError::class)
    fun `not assignable`() {
        I::class `should be assignable from` B::class
    }
}
