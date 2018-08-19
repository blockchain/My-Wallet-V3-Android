package com.blockchain.extensions

import org.amshove.kluent.`should equal`
import org.junit.Test

class IterableExtensionsKtTest {

    @Test
    fun `should return second item in list`() {
        listOf(1, 2, 3, 4, 5).nextAfterOrNull { it == 1 } `should equal` 2
    }

    @Test
    fun `should return last item in list`() {
        listOf(1, 2, 3, 4, 5).nextAfterOrNull { it == 4 } `should equal` 5
    }

    @Test
    fun `should return null as item is at end of list`() {
        listOf(1, 2, 3, 4, 5).nextAfterOrNull { it == 5 } `should equal` null
    }

    @Test
    fun `should return null as predicate doesn't match`() {
        listOf(1, 2, 3, 4, 5).nextAfterOrNull { it == 10 } `should equal` null
    }

    @Test
    fun `should return next value regardless of duplicates`() {
        listOf(1, 2, 1, 3).nextAfterOrNull { it == 1 } `should equal` 2
    }
}