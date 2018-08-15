package com.blockchain.ui.password

interface SecondPasswordHandler {

    interface ResultListener {

        fun onNoSecondPassword()

        fun onSecondPasswordValidated(validatedSecondPassword: String)
    }

    fun validate(listener: ResultListener)
}
