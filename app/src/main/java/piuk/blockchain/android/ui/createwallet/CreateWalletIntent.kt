package piuk.blockchain.android.ui.createwallet

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso

sealed class CreateWalletIntent : Intent<CreateWalletModelState> {
    data class EmailInputChanged(val input: String) : CreateWalletIntent()
    data class PasswordInputChanged(val input: String) : CreateWalletIntent()
    data class PasswordConfirmationInputChanged(val input: String) : CreateWalletIntent()
    data class CountryInputChanged(val countryCode: CountryIso) : CreateWalletIntent()
    data class StateInputChanged(val stateCode: StateIso) : CreateWalletIntent()
    data class ReferralInputChanged(val input: String) : CreateWalletIntent()
    data class TermsOfServiceStateChanged(val isChecked: Boolean) : CreateWalletIntent()
    object NextClicked : CreateWalletIntent() {
        override fun isValidFor(modelState: CreateWalletModelState): Boolean = !modelState.isCreateWalletLoading
    }
    object ErrorHandled : CreateWalletIntent()
}
