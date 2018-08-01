package info.blockchain.utils

/**
 * If empty returns "0", otherwise passes value through
 */
fun String.sanitiseEmptyNumber() = if (isNotEmpty()) this else "0"
