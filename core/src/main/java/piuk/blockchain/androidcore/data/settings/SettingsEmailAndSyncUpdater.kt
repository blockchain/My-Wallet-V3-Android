package piuk.blockchain.androidcore.data.settings

import com.blockchain.nabu.NabuUserSync
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import io.reactivex.Single

internal class SettingsEmailAndSyncUpdater(
    private val settingsDataManager: SettingsDataManager,
    private val nabuUserSync: NabuUserSync
) : EmailSyncUpdater {

    override fun email(): Single<Email> {
        return settingsDataManager.fetchSettings()
            .toJustEmail()
    }

    override fun updateEmailAndSync(email: String): Single<Email> {
        return email()
            .flatMap { existing ->
                if (!existing.verified || existing.address != email) {
                    settingsDataManager.updateEmail(email)
                        .flatMapSingle { settings ->
                            nabuUserSync
                                .syncUser()
                                .andThen(Single.just(settings))
                        }
                        .toJustEmail()
                } else {
                    Single.just(existing)
                }
            }
    }

    override fun resendEmail(email: String): Single<Email> {
        return settingsDataManager
            .updateEmail(email)
            .toJustEmail()
    }
}

private fun Observable<Settings>.toJustEmail() =
    map { Email(it.email ?: "", it.isEmailVerified) }
        .single(Email("", false))
