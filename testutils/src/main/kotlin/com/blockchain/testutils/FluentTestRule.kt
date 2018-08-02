package com.blockchain.testutils

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class Before internal constructor(internal val beforeBlock: () -> Unit)

fun before(beforeBlock: () -> Unit) = Before(beforeBlock)

infix fun Before.after(afterBlock: () -> Unit): TestRule =
    TryFinallyTestRule(this.beforeBlock, afterBlock)

private class TryFinallyTestRule(private val setupBlock: () -> Any, private val teardownBlock: () -> Unit) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    setupBlock()
                    base.evaluate()
                } finally {
                    teardownBlock()
                }
            }
        }
    }
}