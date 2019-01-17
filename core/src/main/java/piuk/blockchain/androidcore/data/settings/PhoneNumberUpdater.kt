package piuk.blockchain.androidcore.data.settings

import io.reactivex.Single

interface PhoneNumberUpdater {

    fun smsNumber(): Single<String>

    fun updateSms(phoneNumber: PhoneNumber): Single<String>

    fun verifySms(code: String): Single<String>
}
