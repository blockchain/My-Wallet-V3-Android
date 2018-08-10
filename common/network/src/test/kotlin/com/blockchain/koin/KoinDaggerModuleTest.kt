package com.blockchain.koin

import org.amshove.kluent.`should be`
import org.junit.Test
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext
import org.koin.standalone.get
import org.koin.test.AutoCloseKoinTest

private interface I
private class A : I

class KoinDaggerModuleTest : AutoCloseKoinTest() {

    @Test
    fun `get returns same bean instance as Koin`() {
        StandAloneContext.startKoin(
            listOf(
                applicationContext {
                    bean { A() }
                        .bind(I::class)
                }
            )
        )
        get<A>() `should be` get<I>()
        get<A>() `should be` DaggerModule.get(I::class)
        get<A>() `should be` DaggerModule.get(A::class)
    }

    object DaggerModule : KoinDaggerModule()
}