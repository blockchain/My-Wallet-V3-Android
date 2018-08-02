package com.blockchain.testutils

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class MockedRetrofitTest(moshi: Moshi, mockWebServer: MockWebServer) {

    val retrofit = Retrofit.Builder()
        .client(OkHttpClient.Builder().build())
        .baseUrl(mockWebServer.url("/").toString())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build() ?: throw NullPointerException("Retrofit builder returned null object")
}