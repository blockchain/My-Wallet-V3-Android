package piuk.blockchain.androidcore.data.settings

import io.reactivex.Single

data class Email(
    val address: String,
    val verified: Boolean
)

interface EmailSyncUpdater {

    fun email(): Single<Email>

    /**
     * Does nothing when email is unchanged and verified.
     * Syncs changes with Nabu.
     */
    fun updateEmailAndSync(email: String): Single<Email>

    /**
     * Always sends a new email, even if verified
     */
    fun resendEmail(email: String): Single<Email>
}
