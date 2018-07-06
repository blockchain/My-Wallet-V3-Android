package piuk.blockchain.androidcore.utils.helperfunctions

import kotlin.reflect.KProperty

/**
 * An adaptation of Kotlin's [SynchronizedLazyImpl] class which allows for the value to be reset
 * and lazily loaded again.
 */
class InvalidatableLazy<T>(private val initializer: () -> T) : Lazy<T> {

    /**
     * Backing property for [value].
     */
    @Volatile
    private var _value: Any? = UninitializedValue
    private val lock = this

    /**
     * Resets the delegate so that on next property access, the original initializer function
     * [initializer] is called again.
     */
    fun invalidate() {
        _value = UninitializedValue
    }

    @Suppress("UNCHECKED_CAST")
    override val value: T
        get() {
            val v1 = _value
            if (v1 !== UninitializedValue) return v1 as T

            return synchronized(lock) {
                val v2 = _value
                if (v2 !== UninitializedValue) {
                    return@synchronized v2 as T
                } else {
                    val typedValue = initializer()
                    _value = typedValue
                    return@synchronized typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String =
        if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        _value = t
    }
}

private object UninitializedValue