package piuk.blockchain.androidcore.utils.annotations

/**
 * This annotation is used both to "open" a class that is otherwise final by default (as is the case
 * with all Kotlin classes) and signify that the class is able to be mocked for testing classes
 * which depend on the annotated class. Note that this annotation also implicitly means that
 * the class was not designed to be extended.
 */
annotation class Mockable