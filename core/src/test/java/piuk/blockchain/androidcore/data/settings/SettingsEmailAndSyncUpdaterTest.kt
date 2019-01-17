package piuk.blockchain.androidcore.data.settings

import com.blockchain.nabu.NabuUserSync
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`it throws`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.junit.Test

class SettingsEmailAndSyncUpdaterTest {

    @Test
    fun `can get unverified email from settings`() {
        val settings: Settings = mock {
            on { email } `it returns` "email@blockchain.com"
            on { isEmailVerified } `it returns` false
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.just(settings)
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .email()
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` "email@blockchain.com"
                verified `should be` false
            }
    }

    @Test
    fun `can get verified email from settings`() {
        val settings: Settings = mock {
            on { email } `it returns` "otheremail@emaildomain.com"
            on { isEmailVerified } `it returns` true
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.just(settings)
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .email()
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` "otheremail@emaildomain.com"
                verified `should be` true
            }
    }

    @Test
    fun `missing settings returns empty email`() {
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.empty()
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .email()
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` ""
                verified `should be` false
            }
    }

    @Test
    fun `can update email in settings`() {
        val oldSettings: Settings = mock {
            on { email } `it returns` "oldemail@blockchain.com"
            on { isEmailVerified } `it returns` false
        }
        val settings: Settings = mock {
            on { email } `it returns` "newemail@blockchain.com"
            on { isEmailVerified } `it returns` false
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.just(oldSettings)
            on { updateEmail(any()) } `it returns` Observable.just(settings)
        }
        val nabuUserSync = expectToSync()
        SettingsEmailAndSyncUpdater(settingsDataManager, nabuUserSync)
            .updateEmailAndSync("newemail@blockchain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` "newemail@blockchain.com"
                verified `should be` false
            }
        verify(settingsDataManager).fetchSettings()
        verify(settingsDataManager).updateEmail("newemail@blockchain.com")
        verifyNoMoreInteractions(settingsDataManager)
        verify(nabuUserSync).syncUser()
    }

    @Test
    fun `can resend email in settings`() {
        val settings: Settings = mock {
            on { email } `it returns` "oldemail@blockchain.com"
            on { isEmailVerified } `it returns` false
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { updateEmail(any()) } `it returns` Observable.just(settings)
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .resendEmail("oldemail@blockchain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` "oldemail@blockchain.com"
                verified `should be` false
            }
        verify(settingsDataManager).updateEmail("oldemail@blockchain.com")
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `if the email is verified, when you try to change it to the same thing, it does not update`() {
        val settings: Settings = mock {
            on { email } `it returns` "theemail@emaildomain.com"
            on { isEmailVerified } `it returns` true
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.just(settings)
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .updateEmailAndSync("theemail@emaildomain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` "theemail@emaildomain.com"
                verified `should be` true
            }
        verify(settingsDataManager).fetchSettings()
        verify(settingsDataManager, never()).updateEmail(any())
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `if the email is not-verified, when you try to change it to the same thing, it does update`() {
        val settings: Settings = mock {
            on { email } `it returns` "theemail@emaildomain.com"
            on { isEmailVerified } `it returns` false
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.just(settings)
            on { updateEmail("theemail@emaildomain.com") } `it returns` Observable.just(settings)
        }
        val nabuUserSync = expectToSync()
        SettingsEmailAndSyncUpdater(settingsDataManager, nabuUserSync)
            .updateEmailAndSync("theemail@emaildomain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should equal` "theemail@emaildomain.com"
                verified `should be` false
            }
        verify(settingsDataManager).fetchSettings()
        verify(settingsDataManager).updateEmail("theemail@emaildomain.com")
        verifyNoMoreInteractions(settingsDataManager)
        verify(nabuUserSync).syncUser()
    }
}

private fun expectToSync(): NabuUserSync =
    mock {
        on { syncUser() } `it returns` Completable.complete()
    }

private fun notExpectingSync(): NabuUserSync =
    mock {
        on { syncUser() } `it throws` RuntimeException("Not expecting to sync the user")
    }
