package com.blockchain.kycui.address.models

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class AddressDialogTest {

    @Test
    fun `initial state should be emitted`() {
        val subject = PublishSubject.create<AddressIntent>()
        AddressDialog(subject, basicAddressModel)
            .viewModel
            .test()
            .assertValue(basicAddressModel)
    }

    @Test
    fun `FirstLine intent should update first line`() {
        val subject = PublishSubject.create<AddressIntent>()
        val testObserver = AddressDialog(subject, basicAddressModel)
            .viewModel
            .skip(1)
            .test()
        val firstLine = "FIRST_LINE"
        subject.onNext(AddressIntent.FirstLine(firstLine))
        testObserver
            .assertValue(basicAddressModel.copy(firstLine = firstLine))
    }

    @Test
    fun `SecondLine intent should update second line`() {
        val subject = PublishSubject.create<AddressIntent>()
        val testObserver = AddressDialog(subject, basicAddressModel)
            .viewModel
            .skip(1)
            .test()
        val secondLine = "SECOND_LINE"
        subject.onNext(AddressIntent.SecondLine(secondLine))
        testObserver
            .assertValue(basicAddressModel.copy(secondLine = secondLine))
    }

    @Test
    fun `City intent should update city`() {
        val subject = PublishSubject.create<AddressIntent>()
        val testObserver = AddressDialog(subject, basicAddressModel)
            .viewModel
            .skip(1)
            .test()
        val city = "CITY"
        subject.onNext(AddressIntent.City(city))
        testObserver
            .assertValue(basicAddressModel.copy(city = city))
    }

    @Test
    fun `State intent should update state`() {
        val subject = PublishSubject.create<AddressIntent>()
        val testObserver = AddressDialog(subject, basicAddressModel)
            .viewModel
            .skip(1)
            .test()
        val state = "STATE"
        subject.onNext(AddressIntent.State(state))
        testObserver
            .assertValue(basicAddressModel.copy(state = state))
    }

    @Test
    fun `PostCode intent should update post code`() {
        val subject = PublishSubject.create<AddressIntent>()
        val testObserver = AddressDialog(subject, basicAddressModel)
            .viewModel
            .skip(1)
            .test()
        val postCode = "POST_CODE"
        subject.onNext(AddressIntent.PostCode(postCode))
        testObserver
            .assertValue(basicAddressModel.copy(postCode = postCode))
    }

    @Test
    fun `Country intent should update country`() {
        val subject = PublishSubject.create<AddressIntent>()
        val testObserver = AddressDialog(subject, basicAddressModel)
            .viewModel
            .skip(1)
            .test()
        val country = "COUNTRY"
        subject.onNext(AddressIntent.Country(country))
        testObserver
            .assertValue(basicAddressModel.copy(country = country))
    }

    private val basicAddressModel: AddressModel
        get() = AddressModel("", null, "", null, "", "")
}