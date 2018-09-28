package com.blockchain.android.testutils

import org.amshove.kluent.`should equal`
import org.junit.Test

class LazyImplTest {

    @Test
    fun `get returns object`() {
        val any = Any()
        LazyImpl(any).get() `should equal` any
    }
}