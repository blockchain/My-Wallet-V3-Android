package com.blockchain.testutils

import org.amshove.kluent.`should be`
import kotlin.reflect.KClass

@Suppress("FunctionName")
infix fun KClass<*>.`should be assignable from`(kClass: KClass<*>) {
    this.java.isAssignableFrom(kClass.java) `should be` true
}
