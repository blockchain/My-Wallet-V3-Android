package com.blockchain.koin

import org.koin.KoinContext
import org.koin.standalone.StandAloneContext
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

/**
 * Temporary to allow Dagger modules to delegate
 * Based on gist: https://gist.github.com/fredy-mederos/b74e8c2f2ca2f0f5d5910bcb694cbdbf
 */
abstract class KoinDaggerModule {

    /**
     * inject lazily given dependency
     */
    @JvmOverloads
    fun <T> inject(clazz: Class<T>, name: String = ""): Lazy<T> {
        return lazy { get(clazz, name) }
    }

    /**
     * inject lazily given dependency
     */
    fun <T : Any> inject(kclazz: KClass<*>, name: String = ""): Lazy<T> {
        return lazy { get<T>(kclazz, name) }
    }

    /**
     * Retrieve given dependency
     */
    @JvmOverloads
    fun <T> get(clazz: Class<T>, name: String = ""): T {
        return get(Reflection.getOrCreateKotlinClass(clazz), name)
    }

    /**
     * Retrieve given dependency
     */
    fun <T> get(kclazz: KClass<*>, name: String = ""): T {
        val koinContext = (StandAloneContext.koinContext as KoinContext)

        val beanDefinition = if (name.isBlank())
            koinContext.beanRegistry.searchAll(kclazz)
        else
            koinContext.beanRegistry.searchByName(name, kclazz)

        return koinContext.resolveInstance(kclazz, { emptyMap() }, { beanDefinition }) as T
    }

    /**
     * inject lazily given property
     */
    @JvmOverloads
    fun <T> property(key: String, defaultValue: T? = null): Lazy<T?> {
        return lazy { getProperty(key, defaultValue) }
    }

    /**
     * Retrieve given property
     */
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun <T> getProperty(key: String, defaultValue: T? = null): T? {
        val koinContext = (StandAloneContext.koinContext as KoinContext)
        return koinContext.propertyResolver.properties[key] as T? ?: defaultValue
    }
}