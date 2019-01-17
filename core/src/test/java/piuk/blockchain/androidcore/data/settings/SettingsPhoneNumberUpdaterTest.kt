package piuk.blockchain.androidcore.data.settings

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.junit.Test

class SettingsPhoneNumberUpdaterTest {

    @Test
    fun `can get number from settings`() {
        val settings: Settings = mock {
            on { smsNumber } `it returns` "+123456"
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.just(settings)
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .smsNumber()
            .test()
            .assertComplete()
            .values()
            .single() `should equal` "+123456"
    }

    @Test
    fun `missing settings returns empty number`() {
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() } `it returns` Observable.empty()
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .smsNumber()
            .test()
            .assertComplete()
            .values()
            .single() `should equal` ""
    }

    @Test
    fun `can update number in settings with sanitised input`() {
        val settings: Settings = mock {
            on { smsNumber } `it returns` "+123456"
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { updateSms(any()) } `it returns` Observable.just(settings)
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .updateSms(PhoneNumber("+(123)-456-789"))
            .test()
            .assertComplete()
            .values()
            .single() `should equal` "+123456"
        verify(settingsDataManager).updateSms("+123456789")
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `can verify code in settings`() {
        val settings: Settings = mock {
            on { smsNumber } `it returns` "+123456"
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { verifySms(any()) } `it returns` Observable.just(settings)
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .verifySms("ABC345")
            .test()
            .assertComplete()
            .values()
            .single() `should equal` "+123456"
        verify(settingsDataManager).verifySms("ABC345")
        verifyNoMoreInteractions(settingsDataManager)
    }
}
