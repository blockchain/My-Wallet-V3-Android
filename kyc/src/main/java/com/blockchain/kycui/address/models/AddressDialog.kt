package com.blockchain.kycui.address.models

import io.reactivex.Observable

class AddressDialog(intents: Observable<AddressIntent>, initial: AddressModel) {

    val viewModel: Observable<AddressModel> =
        intents.scan(initial) { previousState, intent ->
            when (intent) {
                is AddressIntent.FirstLine -> previousState.copy(firstLine = intent.firstLine)
                is AddressIntent.SecondLine -> previousState.copy(secondLine = intent.secondLine)
                is AddressIntent.City -> previousState.copy(city = intent.city)
                is AddressIntent.State -> previousState.copy(state = intent.state)
                is AddressIntent.PostCode -> previousState.copy(postCode = intent.postCode)
                is AddressIntent.Country -> previousState.copy(country = intent.country)
            }
        }
}