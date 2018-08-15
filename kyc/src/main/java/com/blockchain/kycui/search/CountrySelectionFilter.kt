package com.blockchain.kycui.search

import com.blockchain.kycui.countryselection.util.CountryDisplayModel
import io.reactivex.Observable

fun Observable<List<CountryDisplayModel>>.filterCountries(
    query: Observable<CharSequence>
): Observable<List<CountryDisplayModel>> =
    ListQueryObservable(
        Observable.just<CharSequence>("").concatWith(query),
        this
    ).matchingItems { q, list ->
        list.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.countryCode.contains(q, ignoreCase = true)
        }
    }
