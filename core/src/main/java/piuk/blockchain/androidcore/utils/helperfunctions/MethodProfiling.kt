package piuk.blockchain.androidcore.utils.helperfunctions

import timber.log.Timber

/**
 * Prints the time taken for a function to execute. Note that due to limitations in the Kotlin API,
 * the function pointed to is actually the enclosing function that calls the wrapped function.
 * We can get the function name via reflection, but we don't have the reflection libs on the
 * classpath for good reason.
 *
 * @param func The function you wish to profile. As even "void" functions in Kotlin technically
 * return type [Unit], this works for all functions.
 */
fun <T> profile(func: () -> T): T {
    val startTime = System.nanoTime()
    val returnValue = func()
    val finishTime = System.nanoTime()
    val timeTaken = finishTime - startTime
    Timber.d(
        "Function ${func.javaClass.name.replace(
            "$",
            "."
        ).replace(Regex(".[0-9]"), "()")
        } took $timeTaken nanoseconds to execute"
    )
    return returnValue
}