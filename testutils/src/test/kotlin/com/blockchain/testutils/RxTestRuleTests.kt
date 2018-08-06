package com.blockchain.testutils

import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.junit.Test

class RxTestRuleTests {

    @Test
    fun `can set io trampoline`() {
        rxInit { ioTrampoline() }
            .runRule {
                Schedulers.io() `should be` Schedulers.trampoline()
            }
        Schedulers.io() `should not be` Schedulers.trampoline()
    }

    @Test
    fun `can set io to a test scheduler`() {
        val scheduler = TestScheduler()

        rxInit { io(scheduler) }
            .runRule {
                Schedulers.io() `should be` scheduler
            }
        Schedulers.io() `should not be` scheduler
    }

    @Test
    fun `can set computation trampoline`() {
        rxInit { computationTrampoline() }
            .runRule {
                Schedulers.computation() `should be` Schedulers.trampoline()
            }
        Schedulers.computation() `should not be` Schedulers.trampoline()
    }

    @Test
    fun `can set computation to a test scheduler`() {
        val scheduler = TestScheduler()

        rxInit { computation(scheduler) }
            .runRule {
                Schedulers.computation() `should be` scheduler
            }
        Schedulers.computation() `should not be` scheduler
    }

    @Test
    fun `can set single trampoline`() {
        rxInit { singleTrampoline() }
            .runRule {
                Schedulers.single() `should be` Schedulers.trampoline()
            }
        Schedulers.single() `should not be` Schedulers.trampoline()
    }

    @Test
    fun `can set single to a test scheduler`() {
        val scheduler = TestScheduler()

        rxInit { single(scheduler) }
            .runRule {
                Schedulers.single() `should be` scheduler
            }
        Schedulers.single() `should not be` scheduler
    }

    @Test
    fun `can set newThread trampoline`() {
        rxInit { newThreadTrampoline() }
            .runRule {
                Schedulers.newThread() `should be` Schedulers.trampoline()
            }
        Schedulers.newThread() `should not be` Schedulers.trampoline()
    }

    @Test
    fun `can set newThread to a test scheduler`() {
        val scheduler = TestScheduler()

        rxInit { newThread(scheduler) }
            .runRule {
                Schedulers.newThread() `should be` scheduler
            }
        Schedulers.newThread() `should not be` scheduler
    }
}
